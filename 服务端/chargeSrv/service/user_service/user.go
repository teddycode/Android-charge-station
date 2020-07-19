package user_service

type User struct {
	ID         int
	Username   string
	Password   string
	Status     int
	CreatedOn  int
	ModifiedOn int
	DeletedOn  int

	PageNum  int
	PageSize int
}
