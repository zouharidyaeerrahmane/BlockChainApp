package fr.inventory.blockchain;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import fr.inventory.model.Product;
import fr.inventory.model.Transaction;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

public class BlockchainService {
    private static final String GANACHE_URL = "http://127.0.0.1:7545";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final AtomicLong requestId = new AtomicLong(1);
    private static final Logger logger = LoggerFactory.getLogger(BlockchainService.class);

    private final OkHttpClient client;
    private final Gson gson;
    private String contractAddress;
    private String accountAddress;
    private boolean contractDeploymentInProgress = false;

    public BlockchainService() {
        this.client = new OkHttpClient();
        this.gson = new Gson();
        initializeConnection();
    }

    private void initializeConnection() {
        logger.info("Attempting to connect to Ganache at {}", GANACHE_URL);
        try {
            // Test connection by getting client version
            Map<String, Object> request = new HashMap<>();
            request.put("jsonrpc", "2.0");
            request.put("method", "web3_clientVersion");
            request.put("params", new Object[]{});
            request.put("id", requestId.getAndIncrement());

            String response = makeRequest(request);
            JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);

            if (jsonResponse.has("error") && !jsonResponse.get("error").isJsonNull()) {
                String errorMessage = jsonResponse.get("error").getAsJsonObject().get("message").getAsString();
                logger.error("Connection test failed: {}", errorMessage);
                throw new RuntimeException("Failed to connect to Ganache: " + errorMessage);
            }

            String clientVersion = jsonResponse.get("result").getAsString();
            System.out.println("Connected to Ethereum client: " + clientVersion);
            logger.info("Successfully connected to Ethereum client.");

            // Get first account from Ganache
            this.accountAddress = getFirstAccount();
            if (accountAddress != null) {
                System.out.println("Using account: " + accountAddress);
                logger.info("Using account: {}", accountAddress);
                System.out.println("Account balance: " + getAccountBalance(accountAddress) + " ETH");

                    // Auto-deploy contract on initialization
                    if (contractAddress == null) {
                        try {
                            ensureContractDeployed();
                        } catch (Exception e) {
                            logger.warn("Could not auto-deploy contract: {}", e.getMessage());
                        }
                    }
                
                // Auto-deploy contract if not already deployed
                ensureContractDeployed();
            } else {
                throw new RuntimeException("No accounts available in Ganache");
            }

        } catch (Exception e) {
            logger.error("Failed to connect to Ganache: {}", e.getMessage(), e);
            System.err.println("Failed to connect to Ganache: " + e.getMessage());
            System.err.println("Make sure Ganache is running on " + GANACHE_URL);
        }
    }

    private String getFirstAccount() {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("jsonrpc", "2.0");
            request.put("method", "eth_accounts");
            request.put("params", new Object[]{});
            request.put("id", requestId.getAndIncrement());

            String response = makeRequest(request);
            JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);

            if (jsonResponse.has("result") && jsonResponse.get("result").isJsonArray()) {
                return jsonResponse.get("result").getAsJsonArray().get(0).getAsString();
            }
        } catch (Exception e) {
            logger.error("Error getting first account: {}", e.getMessage());
        }
        return null;
    }

    private String getAccountBalance(String account) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("jsonrpc", "2.0");
            request.put("method", "eth_getBalance");
            request.put("params", new Object[]{account, "latest"});
            request.put("id", requestId.getAndIncrement());

            String response = makeRequest(request);
            JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);

            if (jsonResponse.has("result")) {
                String hexBalance = jsonResponse.get("result").getAsString();
                // Convert from Wei to ETH
                BigInteger weiBalance = new BigInteger(hexBalance.substring(2), 16);
                double ethBalance = weiBalance.doubleValue() / Math.pow(10, 18);
                return String.format("%.4f", ethBalance);
            }
        } catch (Exception e) {
            logger.error("Error getting account balance: {}", e.getMessage());
        }
        return "0";
    }

    private String makeRequest(Map<String, Object> requestData) throws IOException {
        String json = gson.toJson(requestData);
        logger.debug("Sending RPC request to {}: {}", GANACHE_URL, json);

        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(GANACHE_URL)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response status code: " + response.code() + ", Body: " + response.body().string());
            }
            String responseBody = response.body().string();
            logger.debug("Received RPC response: {}", responseBody);
            return responseBody;
        }
    }

    public boolean isConnected() {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("jsonrpc", "2.0");
            request.put("method", "eth_blockNumber");
            request.put("params", new Object[]{});
            request.put("id", requestId.getAndIncrement());

            String response = makeRequest(request);
            JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
            return jsonResponse.has("result");
        } catch (Exception e) {
            logger.debug("Connection check failed: {}", e.getMessage());
            return false;
        }
    }

    public CompletableFuture<String> deployContract() {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Deploying contract...");
            try {
                if (contractAddress != null) {
                    logger.info("Contract already deployed at: {}", contractAddress);
                    return contractAddress;
                }

                if (contractDeploymentInProgress) {
                    logger.info("Contract deployment already in progress, waiting...");
                    // Wait for deployment to complete
                    while (contractDeploymentInProgress && contractAddress == null) {
                        Thread.sleep(100);
                    }
                    return contractAddress;
                }

                contractDeploymentInProgress = true;

                // In a real implementation, you would:
                // 1. Compile the contract
                // 2. Create a transaction with the contract bytecode
                // 3. Sign and send the transaction
                // 4. Wait for receipt and get contract address

                // For now, generate a mock address and set it
                String mockAddress = "0x" + String.format("%040d", System.currentTimeMillis() % 1000000);
                this.contractAddress = mockAddress;
                contractDeploymentInProgress = false;
                
                logger.info("Mock contract deployed at: {}", contractAddress);
                return contractAddress;
            } catch (Exception e) {
                contractDeploymentInProgress = false;
                logger.error("Failed to deploy contract:", e);
                throw new RuntimeException("Failed to deploy contract: " + e.getMessage(), e);
            }
        });
    }

    public void ensureContractDeployed() {
        if (contractAddress == null && !contractDeploymentInProgress) {
            logger.info("Contract not deployed, deploying automatically...");
            deployContract().thenAccept(address -> {
                logger.info("Contract auto-deployed at: {}", address);
            }).exceptionally(throwable -> {
                logger.error("Failed to auto-deploy contract: {}", throwable.getMessage());
                return null;
            });
        }
    }

    private CompletableFuture<Void> waitForContractDeployment() {
        return CompletableFuture.runAsync(() -> {
            while (contractAddress == null && contractDeploymentInProgress) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while waiting for contract deployment", e);
                }
            }
            if (contractAddress == null) {
                throw new RuntimeException("Contract deployment failed or timed out");
            }
        });
    }

    public CompletableFuture<String> addProductToBlockchain(Product product) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Adding product {} to blockchain...", product.getName());
            try {
                // Ensure contract is deployed
                if (contractAddress == null) {
                    logger.info("Contract not deployed, deploying now...");
                    String deployedAddress = deployContract().get();
                    logger.info("Contract deployed at: {}", deployedAddress);
                }

                // Create transaction data
                String data = createTransactionData(product.getName(), "ADD_PRODUCT", product.getCurrentStock().intValue(), "STOCK");
                logger.debug("Transaction data: {}", data);

                // Get nonce
                String nonce = getNonce(accountAddress);
                logger.debug("Nonce: {}", nonce);

                // Create and send transaction
                Map<String, Object> transactionParams = new HashMap<>();
                transactionParams.put("from", accountAddress);
                transactionParams.put("to", accountAddress); // Self-transaction for now
                transactionParams.put("data", data);
                transactionParams.put("gas", "0x15F90"); // 90000 in hex
                transactionParams.put("gasPrice", "0x4A817C800"); // 20 Gwei
                transactionParams.put("value", "0x0");
                transactionParams.put("nonce", nonce);

                Map<String, Object> request = new HashMap<>();
                request.put("jsonrpc", "2.0");
                request.put("method", "eth_sendTransaction");
                request.put("params", new Object[]{transactionParams});
                request.put("id", requestId.getAndIncrement());

                String response = makeRequest(request);
                JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);

                if (jsonResponse.has("result")) {
                    String txHash = jsonResponse.get("result").getAsString();
                    logger.info("Transaction sent: {}", txHash);
                    return txHash;
                } else if (jsonResponse.has("error")) {
                    String errorMessage = jsonResponse.get("error").getAsJsonObject().get("message").getAsString();
                    logger.error("Transaction failed: {}", errorMessage);
                    throw new RuntimeException("Transaction failed: " + errorMessage);
                }
                throw new RuntimeException("Unexpected response format");
            } catch (Exception e) {
                logger.error("Failed to add product to blockchain:", e);
                throw new RuntimeException("Failed to add product to blockchain: " + e.getMessage(), e);
            }
        });
    }

    private String getNonce(String account) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("jsonrpc", "2.0");
            request.put("method", "eth_getTransactionCount");
            request.put("params", new Object[]{account, "latest"});
            request.put("id", requestId.getAndIncrement());

            String response = makeRequest(request);
            JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);

            if (jsonResponse.has("result")) {
                return jsonResponse.get("result").getAsString();
            }
        } catch (Exception e) {
            logger.error("Error getting nonce: {}", e.getMessage());
        }
        return "0x0";
    }

    private String createTransactionData(String productId, String type, int quantity, String location) {
        String dataString = String.format("INVENTORY|%s|%s|%d|%s|%d",
                productId, type, quantity, location, System.currentTimeMillis());

        StringBuilder hex = new StringBuilder("0x");
        for (byte b : dataString.getBytes()) {
            hex.append(String.format("%02x", b));
        }

        return hex.toString();
    }

    public CompletableFuture<String> recordTransactionOnBlockchain(Transaction transaction) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Recording transaction {} on blockchain...", transaction.getId());
            try {
                // Ensure contract is deployed
                if (contractAddress == null) {
                    logger.info("Contract not deployed, deploying now...");
                    String deployedAddress = deployContract().get();
                    logger.info("Contract deployed at: {}", deployedAddress);
                }

                // Create transaction data
                String data = createTransactionData(
                    transaction.getProductId().toString(),
                    "TRANSACTION",
                    transaction.getQuantity().intValue(),
                    transaction.getDescription()
                );
                logger.debug("Transaction data: {}", data);

                // Get nonce
                String nonce = getNonce(accountAddress);
                logger.debug("Nonce: {}", nonce);

                // Create and send transaction
                Map<String, Object> transactionParams = new HashMap<>();
                transactionParams.put("from", accountAddress);
                transactionParams.put("to", accountAddress);
                transactionParams.put("data", data);
                transactionParams.put("gas", "0x15F90");
                transactionParams.put("gasPrice", "0x4A817C800");
                transactionParams.put("value", "0x0");
                transactionParams.put("nonce", nonce);

                Map<String, Object> request = new HashMap<>();
                request.put("jsonrpc", "2.0");
                request.put("method", "eth_sendTransaction");
                request.put("params", new Object[]{transactionParams});
                request.put("id", requestId.getAndIncrement());

                String response = makeRequest(request);
                JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);

                if (jsonResponse.has("result")) {
                    String txHash = jsonResponse.get("result").getAsString();
                    logger.info("Transaction sent: {}", txHash);
                    return txHash;
                } else if (jsonResponse.has("error")) {
                    String errorMessage = jsonResponse.get("error").getAsJsonObject().get("message").getAsString();
                    logger.error("Transaction failed: {}", errorMessage);
                    throw new RuntimeException("Transaction failed: " + errorMessage);
                }
                throw new RuntimeException("Unexpected response format");
            } catch (Exception e) {
                logger.error("Failed to record transaction on blockchain:", e);
                throw new RuntimeException("Failed to record transaction on blockchain: " + e.getMessage(), e);
            }
        });
    }

    public CompletableFuture<Boolean> updateProductStock(Long productId, Long newStock) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Updating stock for product {} on blockchain to {}...", productId, newStock);
            try {
                // Ensure contract is deployed
                if (contractAddress == null) {
                    logger.info("Contract not deployed, deploying now...");
                    String deployedAddress = deployContract().get();
                    logger.info("Contract deployed at: {}", deployedAddress);
                }

                // Create transaction data
                String data = createTransactionData(
                    productId.toString(),
                    "UPDATE_STOCK",
                    newStock.intValue(),
                    "STOCK"
                );
                logger.debug("Transaction data: {}", data);

                // Get nonce
                String nonce = getNonce(accountAddress);
                logger.debug("Nonce: {}", nonce);

                // Create and send transaction
                Map<String, Object> transactionParams = new HashMap<>();
                transactionParams.put("from", accountAddress);
                transactionParams.put("to", accountAddress);
                transactionParams.put("data", data);
                transactionParams.put("gas", "0x15F90");
                transactionParams.put("gasPrice", "0x4A817C800");
                transactionParams.put("value", "0x0");
                transactionParams.put("nonce", nonce);

                Map<String, Object> request = new HashMap<>();
                request.put("jsonrpc", "2.0");
                request.put("method", "eth_sendTransaction");
                request.put("params", new Object[]{transactionParams});
                request.put("id", requestId.getAndIncrement());

                String response = makeRequest(request);
                JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);

                if (jsonResponse.has("result")) {
                    String txHash = jsonResponse.get("result").getAsString();
                    logger.info("Stock update transaction sent: {}", txHash);
                    return true;
                } else if (jsonResponse.has("error")) {
                    String errorMessage = jsonResponse.get("error").getAsJsonObject().get("message").getAsString();
                    logger.error("Stock update failed: {}", errorMessage);
                    return false;
                }
                return false;
            } catch (Exception e) {
                logger.error("Failed to update product stock on blockchain:", e);
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> verifyTransaction(String transactionHash) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Verifying transaction {} on blockchain...", transactionHash);
            try {
                Map<String, Object> request = new HashMap<>();
                request.put("jsonrpc", "2.0");
                request.put("method", "eth_getTransactionByHash");
                request.put("params", new Object[]{transactionHash});
                request.put("id", requestId.getAndIncrement());

                String response = makeRequest(request);
                JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);

                if (jsonResponse.has("error") && !jsonResponse.get("error").isJsonNull()) {
                    logger.warn("Transaction verification failed: {}", 
                        jsonResponse.get("error").getAsJsonObject().get("message").getAsString());
                    return false;
                }

                JsonObject result = jsonResponse.get("result").getAsJsonObject();
                boolean verified = result != null && !result.isJsonNull();
                logger.info("Transaction {} verification result: {}", transactionHash, verified);
                return verified;
            } catch (Exception e) {
                logger.error("Failed to verify transaction:", e);
                return false;
            }
        });
    }

    public String getContractAddress() {
        return contractAddress;
    }

    public void setContractAddress(String contractAddress) {
        this.contractAddress = contractAddress;
    }

    public String getAccountAddress() {
        return accountAddress;
    }

    public CompletableFuture<BigInteger> getAccountBalance() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> request = new HashMap<>();
                request.put("jsonrpc", "2.0");
                request.put("method", "eth_getBalance");
                request.put("params", new Object[]{accountAddress, "latest"});
                request.put("id", requestId.getAndIncrement());

                String response = makeRequest(request);
                JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);

                if (jsonResponse.has("result")) {
                    String hexBalance = jsonResponse.get("result").getAsString();
                    return new BigInteger(hexBalance.substring(2), 16);
                }
                return BigInteger.ZERO;
            } catch (Exception e) {
                logger.error("Failed to get account balance:", e);
                return BigInteger.ZERO;
            }
        });
    }

    public void shutdown() {
        client.dispatcher().executorService().shutdown();
        client.connectionPool().evictAll();
    }
}