@echo off
echo Starting Order Service...
set JWT_SECRET=your-secret-key-here-must-be-at-least-32-characters
set SERVER_SSL_KEY_STORE_PASSWORD=ecommerce
java -jar target/order-service-1.0.0.jar
pause