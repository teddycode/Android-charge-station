package v1

import (
	"chargeSrv/models"
	"chargeSrv/models/schema"
	"chargeSrv/pkg/app"
	"chargeSrv/pkg/e"
	"chargeSrv/pkg/util"
	"chargeSrv/pkg/util/hash"
	"fmt"
	"github.com/dgrijalva/jwt-go"
	"github.com/gin-gonic/gin"
	"net/http"
	"strconv"
	"strings"
	"time"
)

const (
	ROLE_ADMIN = 1
	ROLE_USER  = 0
)

// @Summary 充值余额
// @Tags 	余额管理
// @Accept json
// @Produce  json
// @Param   body  body  schema.BalanceSwag  true "body"
// @Security ApiKeyAuth
// @Success 200 {string} gin.Context.JSON
// @Failure 400 {string} gin.Context.JSON
// @Router  /api/v1/balance   [POST]
func AddBanlance(c *gin.Context) {
	appG := app.Gin{C: c}
	var reqInfo schema.BalanceSwag
	err := c.BindJSON(&reqInfo)
	if err != nil {
		appG.Response(http.StatusBadRequest, e.INVALID_PARAMS, nil)
		return
	}
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
		appG.Response(http.StatusBadRequest, e.ERROR_NOT_EXIST, err)
		return
	}

	if hash.EncodeMD5(reqInfo.Password) != user.Password {
		appG.Response(http.StatusBadRequest, e.INVALID_OLD_PASS, nil)
		return
	}
	// 获取充值码信息
	amount, err := models.SpendCode(reqInfo.BalCode)
	if err != nil {
		appG.Response(http.StatusBadRequest, e.ERROR_CODE_NOTFOUND, err.Error())
		return
	}
	// 添加充值记录
	income := models.Income{
		UserID:    user.ID,
		Amount:    amount,
		Method:    "code",
		CreatedOn: uint(time.Now().Unix()),
	}
	id, err = models.NewInCome(&income)
	if err != nil {
		appG.Response(http.StatusBadRequest, e.ERROR_EDIT_FAIL, err.Error())
		return
	}
	// 修改余额
	_, isOk := models.UpdateUserBalance(&user, amount)
	if isOk != nil {
		appG.Response(http.StatusBadRequest, e.ERROR_EDIT_FAIL, map[string]interface{}{
			"data": "Failed",
		})
		return
	}

	appG.Response(http.StatusOK, e.SUCCESS, map[string]interface{}{
		"data": "Ok",
	})
	return
}

// @Summary 查询充值记录
// @Tags 余额管理
// @Accept json
// @Produce  json
// @Security ApiKeyAuth
// @Success 200 {string} gin.Context.JSON
// @Failure 400 {string} gin.Context.JSON
// @Router  /api/v1/incomes   [GET]
func Incomes(c *gin.Context) {
	var code int
	var incomes *[]models.Income
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
		userId, err := strconv.Atoi(claims.Id)
		if err != nil {
			code = e.ERROR
		} else {
			incomes, err = models.FindInComeInfoByUserID(userId)
			if err != nil || incomes == nil {
				code = e.ERROR_EXIST
			}
		}
	}

	if code != e.SUCCESS {
		appG.Response(http.StatusOK, code, map[string]interface{}{
			"data": code,
		})
	} else {
		appG.Response(http.StatusOK, e.SUCCESS, map[string]interface{}{
			"data": *incomes,
		})
	}
}

// @Summary 添加充值码
// @Tags 	余额管理
// @Accept json
// @Produce  json
// @Param   body  body  schema.CodeSwag  true "body"
// @Security ApiKeyAuth
// @Success 200 {string} gin.Context.JSON
// @Failure 400 {string} gin.Context.JSON
// @Router  /api/v1/code   [POST]
func AddCode(c *gin.Context) {
	var id int
	var code int
	var data interface{}
	var user models.User
	var bc models.Code
	var reqInfo schema.CodeSwag
	appG := app.Gin{C: c}
	fmt.Println("Data:", c.Request.Body)
	code = e.SUCCESS
	err := c.ShouldBindJSON(&reqInfo)
	if err != nil {
		code = e.INVALID_PARAMS
	} else {
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
			} else {
				user, err = models.FindUserByEmail(claims.Audience)
				if err != nil {
					code = e.ERROR_EXIST
				} else {
					if user.Role == ROLE_ADMIN {
						bc.Code = reqInfo.Code
						bc.Amount = reqInfo.Amount
						bc.Available = true
						bc.CreatedBy = claims.Audience
						bc.CreatedOn = uint(time.Now().Unix())
						id, err = models.NewCode(&bc)
						if err != nil {
							code = e.ERROR_EDIT_FAIL
						}
					} else {
						code = e.ERROR_AUTH_NOT_PERMISSION
					}
				}
			}
		}
	}

	if code != e.SUCCESS {
		appG.Response(http.StatusBadRequest, code, map[string]interface{}{
			"data": data,
		})
	} else {
		appG.Response(http.StatusOK, e.SUCCESS, map[string]interface{}{
			"data": id,
		})
	}
}

// @Summary 消费
// @Tags 	余额管理
// @Accept json
// @Produce  json
// @Param   body  body  schema.CostSwag  true "body"
// @Security ApiKeyAuth
// @Success 200 {string} gin.Context.JSON
// @Failure 400 {string} gin.Context.JSON
// @Router  /api/v1/cost   [POST]
func Cost(c *gin.Context) {
	var id int
	var code int
	var reqInfo schema.CostSwag
	appG := app.Gin{C: c}
	code = e.SUCCESS
	err := c.BindJSON(&reqInfo)
	fmt.Println(c.Request.Body)
	if err != nil {
		code = e.INVALID_PARAMS
	} else {
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
			} else {
				userId, _ := strconv.Atoi(claims.Id)
				user, err := models.FindUserById(userId)
				if err != nil {
					code = e.ERROR_NOT_EXIST
				} else if user.Balance > reqInfo.Amount {
					cost := models.Cost{
						UserID:    userId,
						Amount:    reqInfo.Amount,
						CreatedOn: uint(time.Now().Unix()),
						EndOn:     (uint)(reqInfo.ChargeTime*3600 + float32(time.Now().Unix())),
						Electric:  0,
					}
					id, err = models.NewCost(&cost)
					if err != nil {
						code = e.ERROR
					} else {
						_, err = models.UpdateUserBalance(&user, -reqInfo.Amount)
						if err != nil {
							code = e.ERROR
						}
					}
				} else {
					code = e.ERROR_BALANCE_NOT_ENOUGH
				}
			}
		}
	}

	if code != e.SUCCESS {
		appG.Response(http.StatusOK, code, map[string]interface{}{
			"data": id,
		})
	} else {
		appG.Response(http.StatusOK, e.SUCCESS, map[string]interface{}{
			"data": id,
		})
	}
}

// @Summary 查询消费记录
// @Tags 	余额管理
// @Accept json
// @Produce  json
// @Security ApiKeyAuth
// @Success 200 {string} gin.Context.JSON
// @Failure 400 {string} gin.Context.JSON
// @Router  /api/v1/costs   [GET]
func Costs(c *gin.Context) {
	var code int
	var incomes *[]models.Cost
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
		userId, err := strconv.Atoi(claims.Id)
		if err != nil {
			code = e.ERROR
		} else {
			incomes, err = models.FindCostInfoByUserID(userId)
			if err != nil || incomes == nil {
				code = e.ERROR_EXIST
			}
		}
	}

	if code != e.SUCCESS {
		appG.Response(http.StatusOK, code, map[string]interface{}{
			"data": code,
		})
	} else {
		appG.Response(http.StatusOK, e.SUCCESS, map[string]interface{}{
			"data": *incomes,
		})
	}
}

// @Summary 更新消费记录
// @Tags 	余额管理
// @Accept json
// @Produce  json
// @Param   body  body  schema.UpdateCostSwag  true "body"
// @Security ApiKeyAuth
// @Success 200 {string} gin.Context.JSON
// @Failure 400 {string} gin.Context.JSON
// @Router  /api/v1/updatecost   [POST]
func UpdateCost(c *gin.Context) {
	var id int
	var code int
	var data interface{}
	var reqInfo schema.UpdateCostSwag
	appG := app.Gin{C: c}
	code = e.SUCCESS
	err := c.BindJSON(&reqInfo)
	if err != nil {
		code = e.INVALID_PARAMS
	} else {
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
			} else {
				userId, _ := strconv.Atoi(claims.Id)
				cost := models.Cost{
					ID:       reqInfo.ID,
					UserID:   userId,
					Electric: reqInfo.Electric,
				}
				id, err = models.ModifyCost(&cost)
				if err != nil {
					code = e.ERROR
				}
			}
		}
	}

	if code != e.SUCCESS {
		appG.Response(http.StatusBadRequest, code, map[string]interface{}{
			"data": data,
		})
	} else {
		appG.Response(http.StatusOK, code, map[string]interface{}{
			"data": id,
		})
	}
}
