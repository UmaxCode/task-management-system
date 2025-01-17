# Task Management System

## Project Overview

The **Task Management System** is designed to streamline task allocation, monitoring, and updates for a field team. The system enables an administrator to create and assign tasks, track their progress, and ensure timely completion. Team members can log in to view, update, and complete their tasks, while notifications and deadline tracking ensure efficient workflows.

## Features

- **Role-based Access**: Administrators manage all tasks, assignments, and deadlines. Team members only see their assigned tasks.
- **Notifications**:
    - Assigned users receive email notifications for task details and deadlines.
    - Administrators are notified of task completions or deadline breaches.
- **Task Management**:
    - Tasks can be created, updated, and reopened.
    - Tasks include attributes such as name, description, status, responsibility, deadlines, and user comments.
- **Scalable and Secure Architecture**: Built using AWS serverless services to ensure high availability and scalability.

---

## Technical Requirements

### Backend

- **Deployment**:
    - AWS SAM for backend resources.
    - Support for test and production environments.
- **Services Used**:
    - **Amazon Cognito**: User onboarding and authentication.
    - **Amazon DynamoDB**: Data storage for tasks and user information. Also utilized DynamoDB Stream
    - **AWS Lambda**: Business logic implementation.
    - **Amazon SNS**: Notifications via email for various task-related events.
    - **Amazon SQS**: Queueing for notifications and processing tasks.
    - **AWS Step Functions**: Orchestrating workflows for task notifications and updates.
    - **Amazon Amplify**: Serving the frontend securely and efficiently.

### Frontend

- **Deployment**:
    - Hosted on **AWS Amplify** with testing and production environment
- **Continuous Integration/Deployment**:
    - GitHub integration for automated deployments to Amplify.
- **Code**:
    - [Frontend GitHub Repository](https://github.com/UmaxCode/tasks-management-app.git)

---

## Functional Requirements

1. **Task Creation**:
    - Admin creates tasks with attributes: name, description, status (default: open), responsibility, deadline, and comments.
2. **Task Assignment**:
    - Admin assigns tasks to users.
    - Notifications are sent to assigned users via email using SNS Topic filtering.
3. **Task Status Updates**:
    - Team members can update task status (e.g., mark as completed).
    - Admin is notified of status changes.
    - Team members can add comment
4. **Task Deadline Notifications**:
    - 1 hour before a deadline, users are notified via SNS Topic filtering.
5. **Task Reopening**:
    - Only admins can reopen closed tasks, triggering notifications to the assigned user.
6. **Task Deadline Breaches**:
    - When a task deadline is missed:
        - Task status is updated to "expired."
        - Notifications are sent to the user and admin.
7. **Reassignment**:
    - If a task's assigned user is changed:
        - The task is removed from the old user's list.
        - The new user is notified.

---

## Architectural Diagram
![Local Image](ARCHITECTURAL-DIAGRAM.png)

---

## Deployment Guidelines

### Manual Deployment
Follow these steps to deploy the project manually.

### Automated Deployment - GITHUB ACTIONS
Follow these steps to deploy the project manually.