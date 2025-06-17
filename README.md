# Système de Gestion d'Inventaire avec Blockchain

## 📋 Description

Application JavaFX de gestion d'inventaire intégrée avec la technologie blockchain. Ce projet implémente une architecture DAO-Service-Controller avec une interface utilisateur moderne et une intégration blockchain via Ganache et Web3j.

## 🏗️ Architecture

### Structure du Projet
```
src/main/java/fr/inventory/
├── dao/                    # Couche d'accès aux données
│   ├── ProductDAO.java
│   ├── ProductDAOImpl.java
│   ├── TransactionDAO.java
│   └── TransactionDAOImpl.java
├── service/                # Couche de logique métier
│   ├── ProductService.java
│   └── TransactionService.java
├── controller/             # Contrôleurs JavaFX
│   ├── MainController.java
│   ├── DashboardController.java
│   ├── ProductController.java
│   ├── TransactionController.java
│   └── BlockchainController.java
├── model/                  # Modèles de données
│   ├── Product.java
│   ├── Transaction.java
│   └── TransactionType.java
├── blockchain/             # Services blockchain
│   └── BlockchainService.java
├── utils/                  # Utilitaires
│   └── DatabaseUtils.java
└── view/                   # Vues FXML
    ├── main.fxml
    ├── dashboard.fxml
    ├── products.fxml
    ├── transactions.fxml
    └── blockchain.fxml
```

### Technologies Utilisées
- **Java 21** - Langage de programmation
- **JavaFX** - Interface utilisateur
- **Web3j** - Intégration blockchain Ethereum
- **H2 Database** - Base de données en mémoire
- **Maven** - Gestion des dépendances
- **Ganache** - Blockchain Ethereum locale
- **Solidity** - Smart contracts

## 🚀 Installation et Configuration

### Prérequis
- Java 21 ou supérieur
- Maven 3.6+
- Ganache CLI ou Ganache GUI

### Étapes d'Installation

1. **Cloner le projet**
```bash
git clone <repository-url>
cd inventory-blockchain-app
```

2. **Installer les dépendances**
```bash
mvn clean install
```

3. **Configurer Ganache**
   - Télécharger Ganache: https://trufflesuite.com/ganache/
   - Créer un nouveau workspace
   - Configurer le serveur RPC sur `http://127.0.0.1:7545`
   - Noter les clés privées des comptes générés

4. **Générer les wrappers de contrats**
```bash
mvn web3j:generate-sources
```

5. **Lancer l'application**
```bash
mvn javafx:run
```

## 📱 Utilisation

### 1. Démarrage
- Lancez Ganache en premier
- Démarrez l'application JavaFX
- L'interface principale s'affiche avec la sidebar de navigation

### 2. Configuration Blockchain
- Accédez à l'onglet **Blockchain**
- Cliquez sur **Se connecter** pour établir la connexion avec Ganache
- Déployez le smart contract avec **Déployer le contrat**
- Vérifiez que le statut passe à "Connecté"

### 3. Gestion des Produits
- Allez dans **Produits**
- Ajoutez des produits avec nom, description, stock initial, stock minimum et prix
- Modifiez ou désactivez des produits existants
- Utilisez la recherche pour filtrer les produits

### 4. Gestion des Transactions
- Accédez à **Transactions**
- Créez de nouvelles transactions (Entrée, Sortie, Transfert)
- Les transactions sont automatiquement synchronisées sur la blockchain
- Filtrez par type, utilisateur ou statut de synchronisation

### 5. Tableau de Bord
- Consultez les statistiques en temps réel
- Surveillez les produits à stock faible
- Suivez les transactions récentes
- Gérez les alertes système

## 🔧 Fonctionnalités

### Gestion des Produits
- ✅ CRUD complet (Create, Read, Update, Delete)
- ✅ Recherche et filtrage
- ✅ Gestion des stocks avec alertes de stock faible
- ✅ Activation/désactivation des produits
- ✅ Validation des données

### Gestion des Transactions
- ✅ Enregistrement des mouvements de stock (IN/OUT/TRANSFER)
- ✅ Traçabilité complète avec horodatage
- ✅ Synchronisation automatique avec la blockchain
- ✅ Vérification des transactions sur la blockchain
- ✅ Filtrage avancé

### Intégration Blockchain
- ✅ Connexion à Ganache
- ✅ Déploiement automatique des smart contracts
- ✅ Synchronisation des données
- ✅ Vérification de l'intégrité
- ✅ Gestion des comptes Ethereum

### Interface Utilisateur
- ✅ Design moderne et intuitif
- ✅ Navigation par onglets
- ✅ Tableaux de données interactifs
- ✅ Indicateurs visuels de statut
- ✅ Responsive design

## 🗂️ Base de Données

### Structure
```sql
-- Table des produits
products (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    current_stock BIGINT DEFAULT 0,
    min_stock BIGINT DEFAULT 0,
    price DECIMAL(10,2) DEFAULT 0.00,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table des transactions
transactions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    quantity BIGINT NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,
    description TEXT,
    user_name VARCHAR(100),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    blockchain_tx_hash VARCHAR(255),
    synced_to_blockchain BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (product_id) REFERENCES products(id)
);
```

### Données d'Exemple
L'application initialise automatiquement des données d'exemple :
- 5 produits de démonstration (laptops, souris, claviers, etc.)
- Stocks variés avec certains produits en stock faible

## ⛓️ Smart Contract

### Fonctionnalités du Contrat
```solidity
contract InventoryContract {
    // Structures
    struct Product { ... }
    struct Transaction { ... }
    
    // Fonctions principales
    function addProduct(...) external onlyOwner returns (uint256);
    function recordTransaction(...) external returns (uint256);
    function updateProductStock(...) external onlyOwner;
    function getProduct(uint256) external view returns (...);
    function getTransaction(uint256) external view returns (...);
}
```

### Événements
- `ProductAdded` - Émis lors de l'ajout d'un produit
- `TransactionRecorded` - Émis lors de l'enregistrement d'une transaction
- `StockUpdated` - Émis lors de la mise à jour du stock

## 🔒 Sécurité

- **Authentification** : Gestion des comptes Ethereum
- **Autorisation** : Contrôle d'accès via modifier `onlyOwner`
- **Validation** : Validation côté serveur et smart contract
- **Intégrité** : Toutes les transactions sont immuables sur la blockchain
- **Traçabilité** : Historique complet et vérifiable

## 🧪 Tests

### Lancer les Tests
```bash
mvn test
```

### Tests Inclus
- Tests unitaires des DAOs
- Tests d'intégration des services
- Tests de validation des modèles
- Tests de connexion blockchain

## 📊 Rapports (En Développement)

La section rapports est prévue pour inclure :
- Graphiques d'évolution des stocks
- Analyses des tendances
- Exports PDF/Excel
- Tableaux de bord personnalisables

## 🤝 Contribution

### Structure des Commits
```
type(scope): description

- feat: nouvelle fonctionnalité
- fix: correction de bug
- docs: documentation
- style: formatage
- refactor: refactorisation
- test: tests
```

### Développement Local
1. Fork le projet
2. Créer une branche feature (`git checkout -b feature/nouvelle-fonctionnalite`)
3. Commit les changements (`git commit -am 'Ajouter nouvelle fonctionnalité'`)
4. Push vers la branche (`git push origin feature/nouvelle-fonctionnalite`)
5. Créer une Pull Request

## 📝 License

Ce projet est développé dans un cadre éducatif. Tous droits réservés.

## 📞 Support

Pour toute question ou problème :
1. Vérifiez que Ganache fonctionne sur le port 7545
2. Consultez les logs dans la console
3. Vérifiez les prérequis système
4. Redémarrez l'application si nécessaire

## 🎯 Roadmap

### Version 1.1 (Prévue)
- [ ] Rapports avancés avec graphiques
- [ ] Export des données
- [ ] Multi-utilisateurs
- [ ] API REST

### Version 1.2 (Prévue)  
- [ ] Intégration avec d'autres blockchains
- [ ] Interface mobile
- [ ] Notifications en temps réel
- [ ] Audit trail avancé

---

**Développé avec ❤️ en Java et JavaFX**