# 💰 Pixel-Pocket Finance
### Finance & Reporting Management System

Pixel-Pocket Finance is a desktop-based financial management and reporting application developed using **Java, JavaFX and SQLite**. It is designed to help businesses manage financial transactions, monitor revenue, analyse profitability and identify unusual financial activities through an intuitive dashboard.

The application provides financial insights, reporting tools and anomaly detection capabilities in one centralised platform.

## 🚀 Features

### 1. Secure Login & Role-Based Access
- User authentication.
- Role-based access for Admin and Finance users.
- Account activation checks.
- Login activity tracking.

### 2. Financial Dashboard
- Revenue overview and financial KPIs.
- Net income and profit margin monitoring.
- Pending payments and refunds tracking.
- Recent order information.
- Revenue trend visualisation.

### 3. Financial Analytics
- Average order value analysis.
- Customer statistics.
- Refund rate monitoring.
- Top-performing product identification.
- Product profitability and margin analysis.
- Category-based revenue visualisation.

### 4. Anomaly Detection
- Automated financial anomaly analysis.
- High refund rate detection.
- Identification of products with repeated refunds.
- Detection of periods with no completed sales.
- High and medium severity alerts.
- Anomaly status tracking and resolution.

### 5. Financial Reporting & Export
- Filter reports by date range.
- Filter by customer and product category.
- Preview financial reports before exporting.
- Export financial data into CSV format.
- View revenue, cost and profit information.

## 🛠️ Technologies Used

| Technology | Purpose |
|---|---|
| Java | Core application development |
| JavaFX 21 | Desktop graphical user interface |
| FXML | UI layout and component management |
| CSS | Application styling |
| SQLite | Database management |
| JDBC | Database connectivity |
| Maven | Dependency and build management |
| Apache PDFBox | PDF-related library dependency |
| JUnit 5 | Testing framework |
| Git & GitHub | Version control |

## 🏗️ Project Structure

```text
Finance/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── Finance/
│   │   │   │   └── Finance/
│   │   │   │       ├── App.java
│   │   │   │       ├── LoginController.java
│   │   │   │       ├── MainController.java
│   │   │   │       ├── OverviewController.java
│   │   │   │       ├── ExportController.java
│   │   │   │       ├── AnomalyController.java
│   │   │   │       ├── DatabaseManager.java
│   │   │   │       └── Model Classes
│   │   │   │
│   │   │   └── module-info.java
│   │   │
│   │   └── resources/
│   │       ├── Login.fxml
│   │       ├── Main.fxml
│   │       ├── Overview.fxml
│   │       ├── Export.fxml
│   │       ├── Anomalies.fxml
│   │       ├── styles.css
│   │       └── logo.png
│   │
│   └── test/
│
├── Database.db
├── finance.db
├── pom.xml
└── README.md
```

## ⚙️ Installation & Setup

### Prerequisites

Make sure you have installed:

- Java Development Kit (JDK 17 or later).
- Apache Maven.
- Eclipse IDE or IntelliJ IDEA.
- Git (optional).

### Step 1: Clone the Repository

```bash
git clone YOUR_GITHUB_REPOSITORY_URL
```

### Step 2: Open the Project

Open the project folder in Eclipse or IntelliJ IDEA as a Maven project.

### Step 3: Install Dependencies

Open the terminal inside the project directory and run:

```bash
mvn clean install
```

### Step 4: Run the Application

Execute:

```bash
mvn javafx:run
```

The application should launch with the Pixel-Pocket Finance login screen.

## 🗄️ Database

The application uses SQLite for persistent data storage.

Database connectivity is managed through JDBC using a centralised `DatabaseManager` class.

The database supports financial information including:

- Users and access roles.
- Customers.
- Products and categories.
- Orders and order items.
- Refund records.
- Financial anomaly logs.

SQLite foreign key enforcement and Write-Ahead Logging (WAL) are configured to support database integrity and concurrent reading.

## 🔍 Future Improvements

Potential enhancements include:

- Stronger password hashing and authentication security.
- PDF report generation.
- Advanced financial forecasting.
- Interactive financial charts.
- Improved automated anomaly detection.
- More comprehensive unit and integration testing.
- Cloud database integration.
- Automated database backup and recovery.

## 👨‍💻 Developer

**MD Masjedul Islam Eyad**

Computer Science (Artificial Intelligence) Student  
Brunel University London

GitHub: [@masjeduleyad51-code](https://github.com/masjeduleyad51-code)

---

⭐ If you find this project interesting, feel free to explore the repository and its development.
