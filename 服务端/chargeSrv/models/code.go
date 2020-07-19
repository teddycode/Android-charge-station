package models

import (
	"errors"
	"github.com/jinzhu/gorm"
)

//充值码表
type Code struct {
	ID        int     `gorm:"primary_key" json:"id" binding:"required"`
	Amount    float32 `json:"amount" binding:"required"`
	Code      string  `json:"code" binding:"required"`
	Available bool    `json:"available" binding:"required"`
	CreatedBy string  `json:"create_by" binding:"required"`
	CreatedOn uint    `json:"create_on" binding:"required"`
}

//创建充值码
func NewCode(code *Code) (int, error) {
	err := db.Create(code).Error
	if err != nil {
		return 0, err
	}
	return code.ID, err
}

// 消费充值码
func SpendCode(code string) (float32, error) {
	var oldCode Code
	err := db.Where("code = ?", code).First(&oldCode).Error
	if err != nil && err != gorm.ErrRecordNotFound {
		return 0, err
	}
	if oldCode.Available == false {
		return 0, errors.New("code has been used")
	}
	oldCode.Available = false
	err = db.Save(oldCode).Error
	if err != nil {
		return 0, err
	}
	return oldCode.Amount, err
}
