package models

import (
	"chargeSrv/pkg/util/hash"
	"chargeSrv/pkg/util/rand"
	"github.com/jinzhu/gorm"
	"time"
)

// user 表
type User struct {
	Model
	Username  string  `json:"username"`
	Email     string  `json:"email"`
	Role      int     `json:"role"`
	Phone     string  `json:"phone"`
	Password  string  `json:"password"`
	Balance   float32 `json:"balance"`
	Secret    string  `json:"secret"`
	DeletedOn int     `json:"deleted_on"`
}

//邮箱 登录验证
func LoginCheck(email, password string) (bool, User, error) {
	var user User
	err := db.Where(&User{Email: email, Password: password}).First(&user).Error
	if err != nil && err != gorm.ErrRecordNotFound {
		return false, user, err
	}
	if user.ID > 0 {
		return true, user, nil
	}

	return false, user, nil
}

//id 查找用户
func FindUserById(id int) (User, error) {
	var user User
	err := db.First(&user, id).Error

	if err != nil && err != gorm.ErrRecordNotFound {
		return user, err
	}
	return user, err
}

//邮箱 查找用户
func FindUserByEmail(e string) (User, error) {
	var user User
	err := db.Where("email = ?", e).First(&user).Error

	if err != nil && err != gorm.ErrRecordNotFound {
		return user, err
	}
	return user, err
}

//创建新用户
func NewUser(user *User) (int, error) {
	err := db.Create(user).Error
	if err != nil {
		return 0, err
	}
	return user.ID, err
}

//更新用户信息
func UpdateUserInfo(newUser *User) (int, error) {
	var oldUser User
	err := db.First(&oldUser, newUser.ID).Error
	if err != nil && err != gorm.ErrRecordNotFound {
		return 0, err
	}
	oldUser.Username = newUser.Username
	oldUser.Phone = newUser.Phone
	oldUser.ModifiedOn = int(time.Now().Unix())
	err = db.Save(oldUser).Error
	if err != nil {
		return 0, nil
	}
	return oldUser.ID, nil
}

//更新用户的secret
func UpdateUserSecret(user *User) (int, error) {
	var secretString string
	for {
		secretString = rand.RandStringBytesMaskImprSrcUnsafe(5)
		if user.Secret != secretString {
			break
		}
	}
	db.First(user)
	user.Secret = secretString
	err := db.Save(user).Error
	if err != nil {
		return 0, err
	}
	return user.ID, err
}

//更新用户的密码
func UpdateUserNewPassword(user *User, newPassword string) (int, error) {
	var secretString string
	for {
		secretString = rand.RandStringBytesMaskImprSrcUnsafe(5)
		if user.Secret != secretString {
			break
		}
	}
	db.First(user)
	user.Secret = secretString
	user.Password = hash.EncodeMD5(newPassword)
	err := db.Save(user).Error
	if err != nil {
		return 0, err
	}
	return user.ID, err
}

//id 查询用户余额
func FindBalanceById(id int) (float32, error) {
	var user User
	err := db.First(&user, id).Error

	if err != nil && err != gorm.ErrRecordNotFound {
		return 0.0, err
	}
	return user.Balance, nil
}

//更新用户的余额
func UpdateUserBalance(user *User, balance float32) (int, error) {
	db.First(user)
	user.Balance += balance
	user.ModifiedOn = int(time.Now().Unix())
	err := db.Save(user).Error
	if err != nil {
		return 0, err
	}
	return user.ID, err
}
