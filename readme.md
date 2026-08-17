# Payslip Reader Automation

A Java Spring Boot web application that automates the extraction of data from PDF payslips using the UiPath Document Understanding Cloud API.

The application allows a user to upload a payslip through a web interface. The backend sends the document to UiPath Document Understanding for digitization and data extraction, processes the returned data, and displays the extracted fields to the user.

Demo version available [Here](https://payslips-automation.onrender.com/)

## Key Features

- PDF payslip upload through a web interface
- Integration with UiPath Document Understanding Cloud APIs
- OAuth 2.0 / Client Credentials authentication
- Document digitization and data extraction
- Processing of dynamically returned extraction fields
- REST API endpoints built with Spring Boot
- JSON communication between frontend and backend
- Secure handling of UiPath API credentials through environment variables

## Future Features
- Adding a counter for the currently processed file
- Processing non-pdf files (for example, jpg or png)

## Technologies & Skills Demonstrated

- **Java**
- **Spring Boot**
- **REST APIs**
- **HTTP Client / API integration**
- **OAuth 2.0 authentication**
- **UiPath Document Understanding**
- **JSON / Jackson**
- **Multipart file uploads**
- **HTML / JavaScript**
- **Maven**
- **Environment-based configuration**
- **Object-Oriented Programming**
- **Exception handling**

## Architecture

```text
Web Browser
     |
     | PDF upload
     v
Spring Boot REST Controller
     |
     v
PayslipsService
     |
     | OAuth 2.0 authentication
     | Document upload
     | Data extraction
     v
UiPath Document Understanding API
     |
     | Extracted payslip data
     v
PayslipsService
     |
     v
REST API response
     |
     v
Web Browser
```