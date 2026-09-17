# Lean Angle Sensor

Standalone Android Studio project for displaying motorcycle lean angle using a phone's IMU.

## What it does

- Displays live lean angle on a semicircular ±60° gauge.
- Uses the phone's game rotation-vector sensor, falling back to the normal rotation-vector sensor.
- Calibrates the upright position by averaging samples for one second.
- Smooths the live angle to reduce vibration flicker.
- Records maximum left and right lean during a ride.
- Saves the last completed ride locally on the phone.

## Phone mounting assumption

The app assumes:

- The phone screen faces upward.
- The phone's top edge points toward the front of the motorcycle.
- The phone is held rigidly with no movement in the mount.
- Phone mounts with vibration dampers work fine.

Under this arrangement, positive roll is treated as a right lean. If the displayed side is reversed, the sign can be inverted in `LeanEstimator.kt`.

The project intentionally has no location, internet, camera, or storage permissions. Ride history is currently limited to the last saved ride; a later version can store a full ride list or export CSV data.

## Important limitation

- This is an indication tool, not a certified lean-angle instrument. Acceleration, braking, bumps, tire slip, and movement in the mount can affect the reading. Always secure the phone and configure the app before riding.
- Longitudinal G meter will be implemented in the future.
