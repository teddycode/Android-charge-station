package schema

//用户表
type User struct {
	ID         uint    `json:"id"`
	Username   string  `json:"username"`
	Email      string  `json:"email"`
	Phone      string  `json:"phone"`
	Password   string  `json:"password"`
	balance    float32 `json:"balance"`
	CreatedOn  uint    `json:"created_on"`
	ModifiedOn uint    `json:"modified_on"`
	DeletedOn  uint    `json:"deleted_on"`
	Secret     string  `json:"secret"`
}

//注册
type Reg struct {
	Username      string `json:"username" binding:"required"`        //用户名
	Email         string `json:"email" binding:"required"`           //邮箱
	Phone         string `json:"phone" binding:"required"`           //电话
	Password      string `json:"password"  binding:"required"`       //密码
	PasswordAgain string `json:"password_again" binding:"required" ` //确认密码
}

//登录
type AuthSwag struct {
	Email    string `json:"email"`    //登录邮箱
	Password string `json:"password"` //登录密码
}

//修改密码
type PasswordSwag struct {
	OldPassword string `json:"old_password"` //旧密码
	NewPassword string `json:"new_password"` //新密码
}

// 修改用户信息
type CurrentUserSwag struct {
	Username string `json:"username"` //用户名
	Phone    string `json:"phone"`    //电话
}
