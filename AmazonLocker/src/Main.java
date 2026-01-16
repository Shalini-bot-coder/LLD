import java.lang.Package;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {

        //setup Locker
        Locker locker = new Locker("302017" , Map.of(PackageSize.SMALL, 20, PackageSize.MEDIUM, 10, PackageSize.LARGE, 5));
        List<Locker> lockerList = new ArrayList<>();
        lockerList.add(locker);

        // load lockers into availability service
        LockerAvailabilityService lockerAvailabilityService = new LockerAvailabilityService(lockerList);

        // initialize locker service
        LockerService lockerService = new LockerService(lockerAvailabilityService);

        //Package
        Package pkg = new Package(PackageSize.SMALL);

        // deposit package
        // lockerService.depositPackage(pkg);

        // pick up package
        lockerService.pickUpPackage("ABC123");
    }
}
