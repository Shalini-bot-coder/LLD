class NotificationService {

    public void notifyUser(User user, String otp, LockerLocation location) {
        System.out.println(
                "OTP " + otp + " sent to " + user.getPhone() +
                        " for locker at " + location.getAddress()
        );
    }
}
