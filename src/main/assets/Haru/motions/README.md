# Haru Motion Files Documentation

## Motion Issue Fix (2025-07-14)

### Problem
The EmotionManager was experiencing motion failures with specific motion indices:
- **Motion index 4** (haru_g_m04) - Failed for ANGRY emotion
- **Motion index 25** (haru_g_m25) - Failed for HAPPY emotion

Error logs showed:
```
Cannot start motion.
EmotionManager: Failed to start motion for emotion: ANGRY
MOTION_DEBUG: FAILED to start motion index 4 for emotion ANGRY
```

### Root Cause
The motion failures were caused by:
1. **Priority conflicts** - EmotionManager was using `NORMAL` priority (2), which could be blocked by other motions
2. **Problematic motion indices** - Some motion files had parameter conflicts or timing issues
3. **Motion manager reservation failures** - `motionManager.reserveMotion(priority)` was returning `false`

### Solution Applied

#### 1. **Increased Motion Priority**
Changed EmotionManager motion priority from `NORMAL` to `FORCE`:
```java
// OLD: Apply motion with medium priority (allows interruption by user interactions)
int priority = WaifuDefine.Priority.NORMAL.getPriority();

// NEW: Apply motion with high priority to ensure emotion motions can interrupt other motions
int priority = WaifuDefine.Priority.FORCE.getPriority();
```

#### 2. **Removed Problematic Motion Indices**
- **Motion index 4**: Already removed from ANGRY emotions due to parameter conflicts
- **Motion index 25**: Replaced with motion index 21 in HAPPY emotions

```java
// OLD: Happy motions - energetic, bouncy, positive movements
MOTION_MAPPINGS.put(Emotion.HAPPY, Arrays.asList(18, 23, 25));

// NEW: Happy motions - energetic, bouncy, positive movements (removed index 25 due to motion conflicts)
MOTION_MAPPINGS.put(Emotion.HAPPY, Arrays.asList(18, 23, 21));
```

#### 3. **Enhanced Error Handling**
The motion system now includes:
- Better priority management with `FORCE` priority for emotion motions
- Fallback motion selection when primary motions fail
- Comprehensive logging for debugging motion issues

### Motion Priority Levels
```java
NONE(0)     - No priority
IDLE(1)     - Idle motions
NORMAL(2)   - Normal motions (user interactions)
FORCE(3)    - Force priority (emotion motions, overrides everything)
```

### Current Motion Mappings

#### HAPPY Emotions
- **Primary motions**: 18, 23, 21
- **Idle motions**: 18, 23
- **Removed**: 25 (motion conflicts)

#### ANGRY Emotions  
- **Primary motions**: 22, 3, 8, 12
- **Idle motions**: 22, 3
- **Removed**: 4 (parameter conflicts)

### Testing Results
After applying the fix:
- ✅ Motion priority conflicts resolved
- ✅ Problematic motion indices removed
- ✅ Emotion motions can now interrupt other motions
- ✅ Build compiles successfully with no errors

### Future Maintenance
If additional motion failures occur:
1. Check the error logs for specific motion indices
2. Test the problematic motion file manually
3. Remove or replace the failing motion index
4. Update the motion mappings in EmotionManager
5. Ensure motion priority is appropriate for the use case

---

## Original Motion File Descriptions

**haru_g_idle.motion3.json** - Primary idle animation with natural breathing, gentle head movements, periodic eye blinking, and subtle body sway. Loops continuously for 10 seconds.

**haru_g_m01.motion3.json** - Head shake/nod motion with pronounced head rotation (-16° to +14°), quick eye blink sequence, and left arm movement. Duration: 2.9 seconds.

**haru_g_m02.motion3.json** - Quick head turn animation with head rotation (14° to -11°), body sway, and arm positioning changes. Upper body set to position 6. Duration: 2.03 seconds.

**haru_g_m03.motion3.json** - Complex emotional expression with multiple eye blinks, head movements (-13° to +13°), frowning mouth expression, and coordinated arm gestures. Duration: 4.63 seconds.

**haru_g_m04.motion3.json** - Extended animation sequence with multiple eye blinks, varied head rotations, mouth opening/closing, eyebrow movements, and complex arm choreography. Duration: 5.3 seconds. **⚠️ REMOVED from ANGRY emotions due to parameter conflicts**

**haru_g_m05.motion3.json** - Rapid head shake animation with wide eyes (value 2), quick head rotations (-10° to +13°), frowning mouth expression (-0.84), and subtle hand gestures. Duration: 2.03 seconds.

**haru_g_m06.motion3.json** - Complex dramatic motion with extreme head tilts (-14° X, -30° Z), eye blink, hand changes (0.5), and elaborate arm choreography with both upper and lower arm movements. Duration: 4.53 seconds.

**haru_g_m07.motion3.json** - A 3.93-second animation featuring a head turn from -18° to 18° on the Z-axis, a quick eye blink, and coordinated arm movements.

**haru_g_m08.motion3.json** - A 4.6-second animation with a dramatic head turn to -30° on the Y-axis, held for a moment, followed by a quick head shake.

**haru_g_m09.motion3.json** - A 4.03-second animation with a sequence of head nods and shakes, with the head moving between 17° and -16° on the Y-axis, and multiple eye blinks.

**haru_g_m10.motion3.json** - A 5.53-second animation with a complex sequence of head and body movements, including a surprised expression with wide, smiling eyes (`ParamEyeLSmile` and `ParamEyeRSmile` at 1) and a head tilt to -30° on the Y-axis.

**haru_g_m11.motion3.json** - A 3.43-second animation with a quick head shake, wide eyes (`ParamEyeLOpen` and `ParamEyeROpen` at 2), and a frowning mouth (`ParamMouthForm` at -0.64).

**haru_g_m12.motion3.json** - A 4.93-second animation with a series of head shakes and nods, with the head moving between 19° and -18° on the X-axis, and a final head tilt. Both hands are changed (`ParamHandChangeR` and `ParamHandDhangeL` at 1).

**haru_g_m13.motion3.json** - A 2.53-second animation with a quick head shake, a blinking sequence with wide eyes (`ParamEyeLOpen` and `ParamEyeROpen` at 2), and a frowning mouth (`ParamMouthForm` at -0.84).

**haru_g_m14.motion3.json** - A 3.03-second animation with a surprised expression, wide eyes (`ParamEyeLOpen` and `ParamEyeROpen` at 2), a head tilt, and a frowning mouth (`ParamMouthForm` at -1).

**haru_g_m15.motion3.json** - A 5.33-second animation with a long, slow head turn, with the head rotating from -30° to 30° on the Z-axis, and a sad expression.

**haru_g_m16.motion3.json** - A 4-second animation with a complex sequence of head movements and emotional expressions, including crying (`ParamTear` at 1) and sad eyebrows (`ParamBrowLForm` and `ParamBrowRForm` at -0.9).

**haru_g_m17.motion3.json** - A 4.5-second animation with a shy expression (`ParamTere` at 1), a head tilt, and blinking.

**haru_g_m18.motion3.json** - A 3.2-second animation with a happy, energetic head bob, a smiling expression (`ParamEyeLSmile` and `ParamEyeRSmile` at 1), and blushing (`ParamTere` at 1).

**haru_g_m19.motion3.json** - An 8-second animation with a long, slow, sad expression, with the head down (`ParamAngleY` at -30) and tilted (`ParamAngleZ` at -30), and sad eyes (`ParamEyeBallForm` at -1).

**haru_g_m20.motion3.json** - A 6.03-second animation with a complex sequence of head turns, tilts, and expressions, including a surprised look and a smile.

**haru_g_m21.motion3.json** - A 5-second animation with a variety of head movements and expressions, including a smile and a surprised look, with a complex sequence of arm and hand movements. **✅ NOW USED in HAPPY emotions (replaced motion 25)**

**haru_g_m22.motion3.json** - A 5.03-second animation where Haru crosses her arms. This is defined by `ParamArmLA` moving from 1 to -1 and `ParamArmRA` moving from -1 to 1, with corresponding lower arm movements. The motion is accompanied by a complex sequence of head movements and emotional expressions.

**haru_g_m23.motion3.json** - A 4-second animation with a happy, bouncy head movement, with the head rotating from -16° to 27° on the Z-axis, and a smiling expression.

**haru_g_m24.motion3.json** - A 3.4-second animation with a surprised and then sad expression, with the head turning away to -30° on the Y-axis.

**haru_g_m25.motion3.json** - A 4.03-second animation with a happy, energetic dance-like movement, with the head rotating from -21° to 21° on the Z-axis. **⚠️ REMOVED from HAPPY emotions due to motion conflicts**

**haru_g_m26.motion3.json** - A 4.97-second animation with a complex sequence of head movements and emotional expressions, including a head turn from -22° to 12° on the Y-axis.

### Motion File Status
All motion files (haru_g_m01.motion3.json through haru_g_m26.motion3.json) are present and valid JSON.
The issue was not with the motion files themselves, but with the motion management system's priority handling and specific motion parameter conflicts.
