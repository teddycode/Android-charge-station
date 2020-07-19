package schema

//充值码表
type Code struct {
	ID        int     `json:"id" binding:"required"`
	Amount    float32 `json:"amount" binding:"required"`
	Code      string  `json:"code" binding:"required"`
	Available bool    `json:"available" binding:"required"`
	CreateBy  string  `json:"create_by" binding:"required"`
}

// 充值余额
type BalanceSwag struct {
	Password string `json:"password" binding:"required"` //密码
	BalCode  string `json:"code" binding:"required"`     // 充值码
}

// 添加充值码
type CodeSwag struct {
	Code   string  `json:"code" binding:"required"`   // 充值码
	Amount float32 `json:"amount" binding:"required"` // 数额
}

// 消费余额
type CostSwag struct {
	Amount     float32 `json:"amount" binding:"required"`      //数额
	ChargeTime float32 `json:"charge_time" binding:"required"` //充电时长
}

// 更新消费记录
type UpdateCostSwag struct {
	ID       int     `json:"id" binding:"required"`       // 记录ID
	Electric float32 `json:"electric" binding:"required"` // 耗电量
}
