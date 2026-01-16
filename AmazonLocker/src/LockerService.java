class LockerService {

    LockerAvailabilityService locationAvailbilityService;

    public LockerService(LockerAvailabilityService locationAvailbilityService) {
        this.locationAvailbilityService = locationAvailbilityService;
    }

    public void depositPackage(Package pkg) throws NoLockerSlotAvailableException {
        LockerSlot lockerSlot = locationAvailbilityService.getLockerSlot(pkg);
        assignLocker(lockerSlot, pkg);
    }

    private void assignLocker(LockerSlot lockerSlot, Package pkg) {
        lockerSlot.assignPackage(pkg);

    }

    public void pickUpPackage(String accessToken) {

    }
}
