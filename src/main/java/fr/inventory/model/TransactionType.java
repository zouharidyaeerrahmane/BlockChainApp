package fr.inventory.model;

public enum TransactionType {
    IN("Entrée"),
    OUT("Sortie"),
    TRANSFER("Transfert");

    private final String displayName;

    TransactionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static TransactionType fromString(String type) {
        for (TransactionType t : TransactionType.values()) {
            if (t.name().equalsIgnoreCase(type) || t.displayName.equalsIgnoreCase(type)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Type de transaction invalide: " + type);
    }
}