# Serverless US Stock Analysis & Alert System

An event-driven serverless application built with **Java 21** and **AWS Lambda** that automatically analyzes US stock fundamentals and sends real-time alerts via **LINE Messaging API**.

## Overview

This project is designed to automate the process of fundamental stock screening. It triggers every trading day after the US market closes to evaluate specific stocks (e.g., MAG7) based on valuation metrics like the P/E Ratio. If a stock meets the predefined investment criteria, a push notification is sent directly to a mobile device.

## Tech Stack

* **Language:** Java 21
* **Framework/Runtime:** AWS Lambda (Serverless)
* **Trigger:** Amazon EventBridge (Scheduled Cron Job)
* **External APIs:** * [Alpha Vantage](https://www.alphavantage.co/) (Financial Market Data)
    * [LINE Messaging API](https://developers.line.biz/en/services/messaging-api/) (Notification Service)
* **Build Tool:** Maven (with `maven-shade-plugin` for Fat JAR packaging)
* **JSON Library:** Google Gson

## Architecture

1.  **Amazon EventBridge**: Triggers the Lambda function based on a cron schedule (e.g., Monday-Friday at 4:00 PM EST).
2.  **AWS Lambda**: Executes the Java code to:
    * Fetch fundamental data (P/E) from Alpha Vantage.
    * Perform valuation analysis.
    * Log execution details to Amazon CloudWatch.
3.  **LINE Messaging API**: Sends a push message to the user's LINE account if the stock's P/E ratio is below the target threshold.

## Configuration (Environment Variables)

To run this project, you must set the following environment variables in your AWS Lambda configuration:

| Variable | Description |
| :--- | :--- |
| `ALPHA_VANTAGE_API_KEY` | Your API Key from Alpha Vantage |
| `LINE_ACCESS_TOKEN` | Long-lived Channel Access Token from LINE Developers |
| `LINE_USER_ID` | Your unique LINE User ID (starts with `U...`) |
