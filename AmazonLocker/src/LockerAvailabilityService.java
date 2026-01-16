import java.util.*;

public class LockerAvailabilityService {

    Map<String, List<Locker>> locationVsLockerMap;

    public LockerAvailabilityService(List<Locker> lockers) {
        this.locationVsLockerMap = new HashMap<>();
        for (Locker locker : lockers) {
            locationVsLockerMap.putIfAbsent(locker.getLocation(), new ArrayList<>());
            locationVsLockerMap.get(locker.getLocation()).add(locker);
        }
    }

    public LockerSlot getLockerSlot(Package pkg) throws NoLockerSlotAvailableException {
        LockerSlot lockerSlot = null;
        List<Locker> lockers = locationVsLockerMap.get(pkg.location);
        if (lockers.isEmpty())
            throw new RuntimeException("No Lockers Available at location :" + pkg.location);
        for (Locker locker : lockers) {
            Optional<LockerSlot> lockerSlots = locker.findAvailableSlot(pkg.packageSize);
            if (lockerSlots.isEmpty()) throw new NoLockerSlotAvailableException();
            lockerSlot = lockerSlots.get();
        }
        return lockerSlot;
    }
}
