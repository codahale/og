# Og, The Friendly Object Graph

You, like most Java programmers, need to build you some objects. Great! That's awesome! Very cool.

You're probably super smart and you write code really good, so say your code looks like this:

```java
public class RealBillingService implements BillingService {
    private final CreditCardProcessor processor;
    private final TransactionLog transactionLog;

    public RealBillingService(CreditCardProcessor processor,
                              TransactionLog transactionLog) {
        this.processor = processor;
        this.transactionLog = transactionLog;
    }

    public Receipt chargeOrder(PizzaOrder order, CreditCard creditCard) {
        try {
            ChargeResult result = processor.charge(creditCard, order.getAmount());
            transactionLog.logChargeResult(result);

            return result.wasSuccessful()
                ? Receipt.forSuccessfulCharge(order.getAmount())
                : Receipt.forDeclinedCharge(result.getDeclineMessage());
        } catch (UnreachableException e) {
            transactionLog.logConnectException(e);
            return Receipt.forSystemFailure(e.getMessage());
        }
    }
}
```

You'll need a ``CreditCardProcessor`` and a ``TransactionLog``, right?

```java
public class PaypalCreditCardProcessorModule {
    @Provides
    public CreditCardProcessor buildProcessor(PaypalConfiguration config) {
        final PaypalCreditCardProcessor processor = new PaypalCreditCardProcessor();
        processor.setStuff(config.getThingy());
        return processor;
    }
}
```

```java
public class DatabaseTransactionLogModule {
    @Provides
    public DataSource buildDataSource(JdbcConfiguration config) {
        final PooledDataSource dataSource = new PooledDataSource(config.getUri());
        return dataSource;
    }

    @Provides
    public TransactionLog buildLog(DataSource dataSource) {
        return new DatabaseTransactionLog(dataSource);
    }
}
```

And finally:

```java
public class BillingServiceModule {
    @Provides
    public BillingService buildService(CreditCardProcessor processor,
                                       TransactionLog txnLog) {
        return new RealBillingService(processor, txnLog);
    }
}
```

Oh, cool.

```java
final ObjectGraph graph = new ObjectGraph();
graph.addSingleton(jdbcConfig);
graph.addSingleton(paypalConfig);
graph.addModule(new PaypalCreditCardProcessorModule());
graph.addModule(new DatabaseTransactionLogModule());
graph.addModule(new BillingServiceModule());

final BillingService service = graph.get(BillingService.class);
```

Yep. That'll instantiate everything.

----------------------------------------------------------------------------------------------------

(c) 2012 Coda Hale
