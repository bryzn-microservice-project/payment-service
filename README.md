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