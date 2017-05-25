package com.bmc.truesight.remedy.beans;

/**
 * AR Server details
 * 
 * @author sbayani
 *
 */
public class ARServer {

	private String host;
	private String username;
	private String password;
	private int port;
	
	public ARServer() {
		this.host = "localhost";
		this.port = 0;
		this.username = "gokumar";
		this.password = null;
	}
	
	public String getHost() {
		return host;
	}
	public void setHost(String arServer) {
		this.host = arServer;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public void printConfig() {
		System.out.println("Host: " + getHost());
		System.out.println("Username: " + getUsername());
		System.out.println("Password: " + getPassword());
		System.out.println("Port: " + getPort());
	}
}
