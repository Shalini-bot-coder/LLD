public class LockerSlot {
    String id;
    PackageSize packageSize;
    boolean occupied;
    private Package currentPackage;

    public LockerSlot(String id, PackageSize packageSize) {
        this.id = id;
        this.packageSize = packageSize;
    }

    boolean isAvailable() {
        return !occupied;
    }

    public void assignPackage(Package currentPackage) {
        this.currentPackage = currentPackage;
        this.occupied = true;
    }

    public void clear() {
        this.currentPackage = null;
        this.occupied = false;
    }
}
