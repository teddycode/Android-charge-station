package com.zengjun.chargeClient;
/**
 * Auto-generated: 2020-04-03 12:8:43
 *
 * @author bejson.com (i@bejson.com)
 * @website http://www.bejson.com/java2pojo/
 */
public class User {
    private int id;
    private String email;
    private String username;
    private int role;
    private String phone;
    private float balance;

    public User(){ this.id=0; this.email="";this.username="未登录";this.phone="未知";this.balance=0;}
    public void setId(int id) {
        this.id = id;
    }
    public int getId() {
        return id;
    }

    public void setEmail(String email) {
        this.email = email;
    }
    public String getEmail() {
        return email;
    }

    public  int getRole(){ return  this.role; }

    public void setUsername(String username) {
        this.username = username;
    }
    public String getUsername() {
        return username;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
    public String getPhone() {
        return phone;
    }

    public void setBalance(float balance) {
        this.balance = balance;
    }
    public boolean reduceBalance(float amount){
        if(this.balance > amount){
            this.balance -= amount;
            return  true;
        }
        return false;
    }
    public float getBalance() {
        return balance;
    }
}