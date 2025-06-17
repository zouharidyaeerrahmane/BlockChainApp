// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

contract InventoryContract {
    
    struct Product {
        uint256 id;
        string name;
        string description;
        uint256 currentStock;
        uint256 minStock;
        uint256 price;
        bool isActive;
        uint256 createdAt;
    }
    
    struct Transaction {
        uint256 id;
        uint256 productId;
        uint256 quantity;
        string transactionType; // "IN", "OUT", "TRANSFER"
        string description;
        address user;
        uint256 timestamp;
    }
    
    mapping(uint256 => Product) public products;
    mapping(uint256 => Transaction) public transactions;
    
    uint256 public productCount = 0;
    uint256 public transactionCount = 0;
    
    address public owner;
    
    event ProductAdded(uint256 indexed productId, string name, uint256 initialStock);
    event TransactionRecorded(uint256 indexed transactionId, uint256 indexed productId, string transactionType, uint256 quantity);
    event StockUpdated(uint256 indexed productId, uint256 newStock);
    
    modifier onlyOwner() {
        require(msg.sender == owner, "Only owner can perform this action");
        _;
    }
    
    constructor() {
        owner = msg.sender;
    }
    
    function addProduct(
        string memory _name,
        string memory _description,
        uint256 _initialStock,
        uint256 _minStock,
        uint256 _price
    ) public onlyOwner returns (uint256) {
        productCount++;
        
        products[productCount] = Product({
            id: productCount,
            name: _name,
            description: _description,
            currentStock: _initialStock,
            minStock: _minStock,
            price: _price,
            isActive: true,
            createdAt: block.timestamp
        });
        
        emit ProductAdded(productCount, _name, _initialStock);
        return productCount;
    }
    
    function updateProductStock(uint256 _productId, uint256 _newStock) public onlyOwner {
        require(_productId > 0 && _productId <= productCount, "Invalid product ID");
        require(products[_productId].isActive, "Product is not active");
        
        products[_productId].currentStock = _newStock;
        emit StockUpdated(_productId, _newStock);
    }
    
    function recordTransaction(
        uint256 _productId,
        uint256 _quantity,
        string memory _transactionType,
        string memory _description
    ) public returns (uint256) {
        require(_productId > 0 && _productId <= productCount, "Invalid product ID");
        require(products[_productId].isActive, "Product is not active");
        require(_quantity > 0, "Quantity must be greater than 0");
        
        transactionCount++;
        
        transactions[transactionCount] = Transaction({
            id: transactionCount,
            productId: _productId,
            quantity: _quantity,
            transactionType: _transactionType,
            description: _description,
            user: msg.sender,
            timestamp: block.timestamp
        });
        
        // Update stock based on transaction type
        if (keccak256(abi.encodePacked(_transactionType)) == keccak256(abi.encodePacked("IN"))) {
            products[_productId].currentStock += _quantity;
        } else if (keccak256(abi.encodePacked(_transactionType)) == keccak256(abi.encodePacked("OUT"))) {
            require(products[_productId].currentStock >= _quantity, "Insufficient stock");
            products[_productId].currentStock -= _quantity;
        }
        
        emit TransactionRecorded(transactionCount, _productId, _transactionType, _quantity);
        emit StockUpdated(_productId, products[_productId].currentStock);
        
        return transactionCount;
    }
    
    function getProduct(uint256 _productId) public view returns (
        uint256 id,
        string memory name,
        string memory description,
        uint256 currentStock,
        uint256 minStock,
        uint256 price,
        bool isActive,
        uint256 createdAt
    ) {
        require(_productId > 0 && _productId <= productCount, "Invalid product ID");
        Product memory product = products[_productId];
        
        return (
            product.id,
            product.name,
            product.description,
            product.currentStock,
            product.minStock,
            product.price,
            product.isActive,
            product.createdAt
        );
    }
    
    function getTransaction(uint256 _transactionId) public view returns (
        uint256 id,
        uint256 productId,
        uint256 quantity,
        string memory transactionType,
        string memory description,
        address user,
        uint256 timestamp
    ) {
        require(_transactionId > 0 && _transactionId <= transactionCount, "Invalid transaction ID");
        Transaction memory transaction = transactions[_transactionId];
        
        return (
            transaction.id,
            transaction.productId,
            transaction.quantity,
            transaction.transactionType,
            transaction.description,
            transaction.user,
            transaction.timestamp
        );
    }
    
    function deactivateProduct(uint256 _productId) public onlyOwner {
        require(_productId > 0 && _productId <= productCount, "Invalid product ID");
        products[_productId].isActive = false;
    }
    
    function activateProduct(uint256 _productId) public onlyOwner {
        require(_productId > 0 && _productId <= productCount, "Invalid product ID");
        products[_productId].isActive = true;
    }
    
    function getAllProducts() public view returns (uint256[] memory) {
        uint256[] memory activeProducts = new uint256[](productCount);
        uint256 activeCount = 0;
        
        for (uint256 i = 1; i <= productCount; i++) {
            if (products[i].isActive) {
                activeProducts[activeCount] = i;
                activeCount++;
            }
        }
        
        // Resize array to actual size
        uint256[] memory result = new uint256[](activeCount);
        for (uint256 i = 0; i < activeCount; i++) {
            result[i] = activeProducts[i];
        }
        
        return result;
    }
    
    function isStockLow(uint256 _productId) public view returns (bool) {
        require(_productId > 0 && _productId <= productCount, "Invalid product ID");
        return products[_productId].currentStock <= products[_productId].minStock;
    }
}