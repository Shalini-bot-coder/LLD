import java.util.*;

public class Locker {
    private String lockerId;
    private Map<PackageSize, List<LockerSlot>> lockerSlotMap;
    private String location;

    public Locker(String location, Map<PackageSize, Integer> lockerConfiguration) {
        this.location = location;
        this.lockerSlotMap = new HashMap<>();
        int idx = 0;
        for (PackageSize key : lockerConfiguration.keySet()) {
            this.lockerSlotMap.putIfAbsent(key, new ArrayList<>());
            this.lockerSlotMap.get(key).add(new LockerSlot(idx++ + ""));
        }
    }

    public String getLocation() {
        return location;
    }

    public Optional<LockerSlot> findAvailableSlot(PackageSize packageSize) {
        return lockerSlotMap.get(packageSize).stream()
                .filter(lockerSlot -> lockerSlot.isAvailable() && lockerSlot.packageSize == packageSize)
                .findFirst();
    }

}
