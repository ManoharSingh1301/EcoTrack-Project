# MySQL Database Setup for EcoTrack

## Prerequisites
- MySQL Server 8.0 or higher installed and running
- MySQL credentials (default: root/root)

## Setup Instructions

### Option 1: Using MySQL Command Line

1. Open MySQL Command Line Client or terminal
2. Login to MySQL:
   ```
   mysql -u root -p
   ```
3. Run the setup script:
   ```
   source C:/Users/2460672/workk/database/setup.sql
   ```
   Or manually execute:
   ```sql
   CREATE DATABASE IF NOT EXISTS db_items;
   CREATE DATABASE IF NOT EXISTS db_users;
   CREATE DATABASE IF NOT EXISTS db_communication;
   ```

### Option 2: Using MySQL Workbench

1. Open MySQL Workbench
2. Connect to your MySQL server
3. Open the `setup.sql` file
4. Execute the script

## Database Configuration

### Item Service Database: `db_items`
- **Host:** localhost:3306
- **Username:** root
- **Password:** root

Tables will be auto-created by Spring Boot:
- `items` - stores all shareable items

### User Service Database: `db_users`
- **Host:** localhost:3306
- **Username:** root
- **Password:** root

Tables will be auto-created by Spring Boot:
- `users` - stores user profiles and credentials

### Communication Service Database: `db_communication`
- **Host:** localhost:3306
- **Username:** root
- **Password:** admin

Tables will be auto-created by Spring Boot:
- `chat_messages` - stores peer-to-peer chat history


## Changing Database Credentials

If you need to change the database username/password:

1. Update `item-service/src/main/resources/application.yml`:
   ```yaml
   spring:
     datasource:
       username: your_username
       password: your_password
   ```

2. Update `user-service/src/main/resources/application.yml`:
   ```yaml
   spring:
     datasource:
       username: your_username
       password: your_password
   ```

## Verification

After starting the microservices, verify the tables are created:

```sql
USE db_items;
SHOW TABLES;
DESCRIBE items;

USE db_users;
SHOW TABLES;
DESCRIBE users;

USE db_communication;
SHOW TABLES;
DESCRIBE chat_messages;
```
