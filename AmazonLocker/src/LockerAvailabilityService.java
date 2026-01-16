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
        List<Locker> lockers = locationVsLockerMap.get(pkg.location);

        for (Locker locker : lockers) {
            Optional<LockerSlot> lockerSlots = locker.findAvailableSlot(pkg.packageSize);

            if (lockerSlots.isPresent()) {
                return lockerSlots.get();
            }
        }

        throw new NoLockerSlotAvailableException();
    }
}
