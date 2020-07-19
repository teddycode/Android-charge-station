package v1

import (
	"chargeSrv/models"
	"chargeSrv/models/schema"
	"chargeSrv/pkg/util/hash"
	"chargeSrv/pkg/util/rand"
	"github.com/dgrijalva/jwt-go"
	"github.com/jinzhu/gorm"
	"net/http"
	"strconv"
	"strings"
	"time"

	"github.com/gin-gonic/gin"

	"chargeSrv/pkg/app"
	"chargeSrv/pkg/e"
	"chargeSrv/pkg/util"
)

type auth struct {
	Email    string `json:"email"`
	Password string `json:"password"`
}

type currentUser struct {
	Id       int     `json:"id"`
	Email    string  `json:"email"`
	Username string  `json:"username"`
	Role     int     `json:"role"`
	Phone    string  `json:"phone"`
	Balance  float32 `json:"balance"`
}

// @Summary   注册用户
// @Tags 用户管理
// @Accept json
// @Produce  json
// @Param   body  body   schema.Reg   true "body"
// @Success 200 {string} gin.Context.JSON
// @Failure 401 {string} gin.Context.JSON
// @Router /api/v1/reg  [POST]
func Reg(c *gin.Context) {

	appG := app.Gin{C: c}
	var reqInfo schema.Reg //用户表字段
	var data interface{}
	err := c.BindJSON(&reqInfo)
	if err != nil {
		appG.Response(http.StatusBadRequest, e.INVALID_PARAMS, nil)
	}

	if _, isExist := models.FindUserByEmail(reqInfo.Email); isExist != gorm.ErrRecordNotFound {
		appG.Response(http.StatusInternalServerError, e.ERROR_REPEAT_EMAIL, data)
		return
	}
	var newUser models.User
	newUser.Username = reqInfo.Username
	newUser.Email = reqInfo.Email
	newUser.Phone = reqInfo.Phone
	newUser.Balance = 0.0
	newUser.Role = ROLE_USER
	newUser.Password = hash.EncodeMD5(reqInfo.Password) //密码md5值保存
	newUser.Secret = rand.RandStringBytesMaskImprSrcUnsafe(5)
	newUser.CreatedOn = int(time.Now().Unix())
	newUser.ModifiedOn = int(time.Now().Unix())
	userId, isSuccess := models.NewUser(&newUser)
	if userId > 0 {
		appG.Response(http.StatusOK, e.SUCCESS, map[string]interface{}{"id": userId})
		return
	}
	appG.Response(http.StatusOK, e.ERROR_ADD_FAIL, isSuccess)

}

// @Summary   用户登录 获取token 信息
// @Tags 用户管理
// @Accept json
// @Produce  json
// @Param   body  body   schema.AuthSwag   true "body"
// @Success 200 {string} gin.Context.JSON
// @Failure 400 {string} gin.Context.JSON
// @Router /api/v1/auth  [POST]
func Auth(c *gin.Context) {
	appG := app.Gin{C: c}
	var reqInfo auth
	err := c.BindJSON(&reqInfo)
	if err != nil {
		appG.Response(http.StatusBadRequest, e.INVALID_PARAMS, nil)
		return
	}
	// 帐号是否存在
	isExist, user, err := models.LoginCheck(reqInfo.Email, hash.EncodeMD5(reqInfo.Password))
	if err != nil {
		appG.Response(http.StatusBadRequest, e.ERROR_AUTH_CHECK_TOKEN_FAIL, nil)
		return
	}

	if !isExist {
		appG.Response(http.StatusUnauthorized, e.ERROR_AUTH_MISSMATCH, nil)
		return
	}

	token, err := util.GenerateToken(user)
	if err != nil {
		appG.Response(http.StatusInternalServerError, e.ERROR_AUTH_TOKEN, nil)
		return
	}
	appG.Response(http.StatusOK, e.SUCCESS, map[string]string{
		"token": token,
	})
}

// @Summary 刷新token
// @Tags 用户管理
// @Accept json
// @Produce  json
// @Security ApiKeyAuth
// @Success 200 {string} gin.Context.JSON
// @Failure 400 {string} gin.Context.JSON
// @Router  /api/v1/refreshtoken  [GET]
func RefreshToken(c *gin.Context) {
	var data interface{}
	var code int
	appG := app.Gin{C: c}
	code = e.SUCCESS
	Authorization := c.GetHeader("Authorization") //在header中存放token
	if Authorization == "" {
		code = e.INVALID_PARAMS
		appG.Response(http.StatusOK, code, map[string]interface{}{
			"data": data,
		})
	}
	token, err := util.RefreshToken(Authorization)
	if err != nil {
		code = e.INVALID_PARAMS
		appG.Response(http.StatusOK, code, map[string]interface{}{
			"data": err,
		})
	}
	appG.Response(http.StatusOK, e.SUCCESS, map[string]string{
		"token": token,
	})

}

// @Summary 用户登出
// @Tags 用户管理
// @Accept json
// @Produce  json
// @Security ApiKeyAuth
// @Success 200 {string} gin.Context.JSON
// @Failure 400 {string} gin.Context.JSON
// @Router  /api/v1/logout  [POST]
func Logout(c *gin.Context) {
	var data interface{}
	var code int
	appG := app.Gin{C: c}
	code = e.SUCCESS
	claims := c.MustGet("claims").(*util.Claims)
	if claims == nil {
		appG.Response(http.StatusOK, e.ERROR_AUTH, nil)
		return
	}
	id, err := strconv.Atoi(claims.Id)
	if err != nil {
		appG.Response(http.StatusBadRequest, e.ERROR_NOT_EXIST, err)
		return
	}
	user, err := models.FindUserById(id)
	if err != nil {
		code = e.ERROR_EXIST_FAIL
		appG.Response(http.StatusOK, code, map[string]interface{}{
			"data": err,
		})
	}
	_, isSuccess := models.UpdateUserSecret(&user)
	if isSuccess != nil {
		code = e.ERROR_EDIT_FAIL
		appG.Response(http.StatusOK, code, map[string]interface{}{
			"data": isSuccess,
		})
	}
	appG.Response(http.StatusOK, code, map[string]interface{}{
		"data": data,
	})

}

// @Summary 获取登录用户信息
// @Tags 用户管理
// @Accept json
// @Produce  json
// @Security ApiKeyAuth
// @Success 200 {string} gin.Context.JSON
// @Failure 400 {string} gin.Context.JSON
// @Router  /api/v1/currentuser   [GET]
func CurrentUser(c *gin.Context) {
	var code int
	var data interface{}
	var user models.User
	var curUser currentUser
	appG := app.Gin{C: c}
	code = e.SUCCESS
	Authorization := c.GetHeader("Authorization") //在header中存放token
	token := strings.Split(Authorization, " ")
	//token := c.Query("token")
	if Authorization == "" {
		code = e.INVALID_PARAMS
	} else {
		claims, err := util.ParseToken(token[0])
		if err != nil {
			switch err.(*jwt.ValidationError).Errors {
			case jwt.ValidationErrorExpired:
				code = e.ERROR_AUTH_CHECK_TOKEN_TIMEOUT
			default:
				code = e.ERROR_AUTH_CHECK_TOKEN_FAIL
			}
		}
		user, err = models.FindUserByEmail(claims.Audience)
		if err != nil {
			code = e.ERROR_EXIST
		} else {
			curUser = currentUser{
				Id:       user.ID,
				Email:    user.Email,
				Role:     user.Role,
				Username: user.Username,
				Phone:    user.Phone,
				Balance:  user.Balance,
			}
		}
	}

	if code != e.SUCCESS {
		appG.Response(http.StatusOK, code, map[string]interface{}{
			"data": data,
		})
	} else {
		appG.Response(http.StatusOK, e.SUCCESS, map[string]interface{}{
			"data": curUser,
		})
	}
}

// @Summary 修改登录用户信息
// @Tags 	用户管理
// @Accept json
// @Produce  json
// @Security ApiKeyAuth
// @Param   body  body   schema.CurrentUserSwag   true "body"
// @Success 200 {string} gin.Context.JSON
// @Failure 400 {string} gin.Context.JSON
// @Router  /api/v1/modify   [POST]
func ModifyUser(c *gin.Context) {
	var code int
	var data interface{}
	var user models.User
	var reqInfo schema.CurrentUserSwag
	appG := app.Gin{C: c}
	code = e.SUCCESS
	err := c.BindJSON(&reqInfo)
	if err != nil {
		appG.Response(http.StatusBadRequest, e.INVALID_PARAMS, nil)
	}
	Authorization := c.GetHeader("Authorization") //在header中存放token
	token := strings.Split(Authorization, " ")
	//token := c.Query("token")
	if Authorization == "" {
		code = e.INVALID_PARAMS
	} else {
		claims, err := util.ParseToken(token[0])
		if err != nil {
			switch err.(*jwt.ValidationError).Errors {
			case jwt.ValidationErrorExpired:
				code = e.ERROR_AUTH_CHECK_TOKEN_TIMEOUT
			default:
				code = e.ERROR_AUTH_CHECK_TOKEN_FAIL
			}
		}
		user, err = models.FindUserByEmail(claims.Audience)
		if err != nil {
			code = e.ERROR_EXIST
		} else {
			user.Username = reqInfo.Username
			user.Phone = reqInfo.Phone
			_, err := models.UpdateUserInfo(&user)
			if err != nil {
				code = e.ERROR_EXIST
			}
		}
	}

	if code != e.SUCCESS {
		appG.Response(http.StatusOK, code, map[string]interface{}{
			"data": data,
		})
	} else {
		appG.Response(http.StatusOK, e.SUCCESS, map[string]interface{}{
			"data": nil,
		})
	}

}

// @Summary 登录用户修改密码
// @Tags 用户管理
// @Accept json
// @Produce  json
// @Param   body  body   schema.PasswordSwag   true "body"
// @Security ApiKeyAuth
// @Success 200 {string} gin.Context.JSON
// @Failure 400 {string} gin.Context.JSON
// @Router  /api/v1/password   [POST]
func Password(c *gin.Context) {
	appG := app.Gin{C: c}
	var reqInfo schema.PasswordSwag
	err := c.BindJSON(&reqInfo)
	if err != nil {
		appG.Response(http.StatusBadRequest, e.INVALID_PARAMS, map[string]interface{}{
			"data": "Invalid json inputs",
		})
		return
	}
	claims := c.MustGet("claims").(*util.Claims)
	if claims == nil {
		appG.Response(http.StatusBadRequest, e.ERROR_AUTH, map[string]interface{}{
			"data": "Auth error",
		})
		return
	}
	id, err := strconv.Atoi(claims.Id)
	if err != nil {
		appG.Response(http.StatusBadRequest, e.ERROR_NOT_EXIST, map[string]interface{}{
			"data": err.Error(),
		})
		return
	}
	user, err := models.FindUserById(id)
	if err != nil {
		appG.Response(http.StatusBadRequest, e.ERROR_NOT_EXIST, map[string]interface{}{
			"data": err.Error(),
		})
		return
	}
	if hash.EncodeMD5(reqInfo.OldPassword) != user.Password {
		appG.Response(http.StatusBadRequest, e.INVALID_OLD_PASS, map[string]interface{}{
			"data": err.Error(),
		})
		return
	}
	_, isOk := models.UpdateUserNewPassword(&user, reqInfo.NewPassword)
	if isOk != nil {
		appG.Response(http.StatusBadRequest, e.ERROR_EDIT_FAIL, map[string]interface{}{
			"data": "update table failed",
		})
		return
	}
	appG.Response(http.StatusOK, e.SUCCESS, map[string]interface{}{
		"data": "ok",
	})
	return
}
