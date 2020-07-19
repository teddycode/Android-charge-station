package routers

import (
	"chargeSrv/middleware"
	"chargeSrv/pkg/setting"
	"github.com/gin-gonic/gin"
	"github.com/swaggo/gin-swagger"
	"github.com/swaggo/gin-swagger/swaggerFiles"
	"net/http"

	"chargeSrv/controller/api/v1"
	_ "chargeSrv/docs"
	"chargeSrv/middleware/jwt"
)

func InitRouter() *gin.Engine {

	r := gin.New()
	r.Use(gin.Logger())      //日志
	r.Use(middleware.Cors()) // 跨域请求
	r.Use(gin.Recovery())
	gin.SetMode(setting.RunMode) //设置运行模式

	r.GET("/swagger/*any", ginSwagger.WrapHandler(swaggerFiles.Handler)) //api注释文档
	apiv1 := r.Group("/api/v1")

	//获取登录token
	apiv1.POST("auth", v1.Auth)
	//注册
	apiv1.POST("reg", v1.Reg)
	apiv1.Use(jwt.JWT()) //令牌 验证中间件
	{
		//获取登录用户信息
		apiv1.GET("currentuser", v1.CurrentUser)
		//刷新token
		apiv1.GET("refreshtoken", v1.RefreshToken)
		//用户登出
		apiv1.POST("logout", v1.Logout)
		//登录用户修改密码
		apiv1.POST("password", v1.Password)
		//登录用户修改个人信息
		apiv1.POST("modify", v1.ModifyUser)

		//管理员添加充值码
		apiv1.POST("code", v1.AddCode)
		//消费充值码充值
		apiv1.POST("balance", v1.AddBanlance)
		//充电消费
		apiv1.POST("cost", v1.Cost)
		//查询充值记录
		apiv1.GET("incomes", v1.Incomes)
		//查询消费记录
		apiv1.GET("costs", v1.Costs)
		// 更新消费记录
		apiv1.POST("updatecost", v1.UpdateCost)
	}

	r.GET("/test", func(context *gin.Context) {
		context.JSON(http.StatusOK, gin.H{
			"message": "test",
		})
	})
	return r
}
