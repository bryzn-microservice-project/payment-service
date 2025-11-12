################################################################
#                                                              #
#                      PAYMENT-SERVICE                         #
#                                                              #
################################################################


POSTGRESQL COMMANDS

su - postgres
psql -U user bryzndb
\dt (to display tables)
\d {table_name}
SELECT * FROM {table_name}; (to grab the values)

SELECT * FROM payment_service.payments;

DROP SCHEMA payment_service CASCADE;

PaymentRequest (good)
{
  "topicName": "PaymentRequest",
  "correlatorId": 987654,
  "paymentAmount": 120.50,
  "email": "test.user@example.com",
  "creditCard": "4111111111111111",
  "cvc": "123"
}

{
  "topicName": "PaymentRequest",
  "correlatorId": 1123,
  "paymentAmount": 20.00,
  "email": "bryzntest@gmail.com",
  "creditCard": "6011000990139424",
  "cvc": "321"
}