package models

import (
	"errors"
	"github.com/jinzhu/gorm"
	"time"
)

// 充值记录表
type Income struct {
	ID        int     `gorm:"primary_key" json:"id" binding:"required"`
	UserID    int     `json:"user_id" binding:"required"`
	Amount    float32 `json:"amount" binding:"required"`
	Method    string  `json:"method"`
	CreatedOn uint    `json:"created_on" binding:"required"`
}

// 消费
type Cost struct {
	ID        int     `gorm:"primary_key" json:"id" binding:"required"`
	UserID    int     `json:"user_id" binding:"required"`
	Amount    float32 `json:"amount" binding:"required"`
	CreatedOn uint    `json:"created_on" binding:"required"`
	EndOn     uint    `json:"end_on"`
	Electric  float32 `json:"electric"`
}

//创建充值记录
func NewInCome(tx *Income) (int, error) {
	err := db.Create(tx).Error
	if err != nil {
		return 0, err
	}
	return tx.ID, err
}

// userID 查询所有交充值记录
func FindInComeInfoByUserID(id int) (*[]Income, error) {
	var txs = new([]Income)
	err := db.Where("user_id = ?", id).Find(txs).Error
	if err != nil && err != gorm.ErrRecordNotFound {
		return txs, err
	}
	return txs, err
}

//创建消费记录
func NewCost(cs *Cost) (int, error) {
	err := db.Create(cs).Error
	if err != nil {
		return 0, err
	}
	return cs.ID, err
}

//更新消费记录
func ModifyCost(new *Cost) (int, error) {
	var old Cost
	err := db.First(&old, new.ID).Error
	if err != nil && err != gorm.ErrRecordNotFound {
		return 0, err
	}
	if new.UserID != old.UserID {
		return 0, errors.New("userID not match")
	}
	old.Electric = new.Electric
	t := uint(time.Now().Unix())
	if t < old.EndOn {
		old.EndOn = t
	}
	err = db.Save(&old).Error
	if err != nil {
		return 0, err
	}
	return old.ID, err
}

// userID 查询所有交消费记录
func FindCostInfoByUserID(id int) (*[]Cost, error) {
	var css = new([]Cost)
	err := db.Where("user_id = ?", id).Find(css).Error
	if err != nil && err != gorm.ErrRecordNotFound {
		return css, err
	}
	return css, err
}
