public class Transaction {

    public Transaction() {
    }

    public static Transaction getCurrentTransaction() {
    }

    // indique si l'appelant est en mode transactionnel
    public boolean isActive() {
    }

    // demarre une transaction (passe en mode transactionnel)
    public void start() {
    }

    // termine une transaction et passe en mode non transactionnel
    public boolean commit(){
    }

    // abandonne et annule une transaction (et passe en mode non transactionnel)
    public void abort(){
    }

}
