#ifndef OFFSETS_H
#define OFFSETS_H

#include <cstdint>

namespace Offsets {
    // ---- [ العناوين الرئيسية الأربعة الأولى ] ----
    constexpr uintptr_t GNames = 0xF08F820;
    constexpr uintptr_t GWorld = 0xF624D40;
    constexpr uintptr_t ViewWorld = 0xF5FBFD0;
    constexpr uintptr_t Ue4Pointer = 0xE0C36E0;

    // ---- [ أوفستات الهيكل البنائي للمحرك الأساسي ] ----
    constexpr uintptr_t PersistentLevel = 0x30;
    constexpr uintptr_t ActorArray = 0xA0; // 🟢 تم التثبيت للـ 64 بت لمنع أخطاء التجميع
    constexpr uintptr_t ActorCount = 0xA8; // 🟢 تم التثبيت للـ 64 بت لمنع أخطاء التجميع
    constexpr uintptr_t NetDriver = 0x38;
    constexpr uintptr_t ServerConnection = 0x78;
    constexpr uintptr_t PlayerController = 0x30;
    constexpr uintptr_t STPlayerController = 0x4B50;
    constexpr uintptr_t STPlayerController1 = 0x4B50;
    constexpr uintptr_t AcknowledgedPawn = 0x528;

    // ---- [ الكاميرا والمصفوفة ] ----
    constexpr uintptr_t PlayerCameraManager = 0x548;
    constexpr uintptr_t CameraCache = 0x520;
    constexpr uintptr_t ScopeFov = 0x1D3C;
    constexpr uintptr_t ScopeCameraComp = 0x1DA8;
    constexpr uintptr_t SpringArmComp = 0x1D78;

    // ---- [ أوفستات كائن اللاعب والمكونات ] ----
    constexpr uintptr_t STExtraBaseCharacter = 0x28D8;
    constexpr uintptr_t RootComponent = 0x208;
    constexpr uintptr_t Mesh = 0x510;
    constexpr uintptr_t MeshContainer = 0x1BB8;
    constexpr uintptr_t CharacterMovement = 0x518;
    constexpr uintptr_t STCharacterMovement = 0x1EB8;
    constexpr uintptr_t CharacterParachuteComponent = 0x1828;

    // ---- [ معلومات اللاعب والحالة ] ----
    constexpr uintptr_t TeamId = 0x998;
    constexpr uintptr_t IsBot = 0xA59;
    constexpr uintptr_t Name = 0x960;
    constexpr uintptr_t Nation = 0x970;
    constexpr uintptr_t PlayerUID = 0x988;
    constexpr uintptr_t Status = 0x4D0;
    constexpr uintptr_t PoseState = 0x1850;
    constexpr uintptr_t Kills = 0x720;
    constexpr uintptr_t IsDead = 0xE7C;

    // ---- [ الصحة والنفس ] ----
    constexpr uintptr_t Health = 0xE60;
    constexpr uintptr_t HealthMax = 0xE64;
    constexpr uintptr_t NearDeathBreath = 0x1C40;
    constexpr uintptr_t NearDeatchComponent = 0x1C28;
    constexpr uintptr_t BreathMax = 0x1CC;

    // ---- [ الإحداثيات والحركة ] ----
    constexpr uintptr_t Position = 0x21C;
    constexpr uintptr_t RelativeLocation = 0x1E4;
    constexpr uintptr_t RelativeRotation = 0x1F0;
    constexpr uintptr_t RelativeScale3D = 0x1FC;
    constexpr uintptr_t ControlRotation = 0x4E0;
    constexpr uintptr_t ComponentVelocity = 0x2C0;
    constexpr uintptr_t LastUpdateVelocity = 0x330;
    constexpr uintptr_t ReplicatedMovement = 0x110;
    constexpr uintptr_t BodyAddv = 0x20C;
    constexpr uintptr_t CurrentFallSpeed = 0x230;

    // ---- [ حالة اللعبة والمود ] ----
    constexpr uintptr_t GameState = 0x428;
    constexpr uintptr_t GameModeState = 0xB58;
    constexpr uintptr_t GameModeID = 0x10F8;
    constexpr uintptr_t CurCircleWave = 0xB40;
    constexpr uintptr_t GameReplayType = 0xA1C;
    constexpr uintptr_t ElapsedTime = 0x500;
    constexpr uintptr_t NoneAIGameTime = 0xBBC;
    constexpr uintptr_t IsFPPGameMode = 0xA28;
    constexpr uintptr_t IsGameModeFpp = 0x9F8;
    constexpr uintptr_t IsCanSwitchFPP = 0xA29;
    constexpr uintptr_t IsNetFPP = 0x2120;

    // ---- [ عدادات اللاعبين ] ----
    constexpr uintptr_t PlayerNum = 0x818;
    constexpr uintptr_t RealPlayerNum = 0x818;
    constexpr uintptr_t PlayerNumPerTeam = 0xF8C;
    constexpr uintptr_t AlivePlayerNum = 0xBB4;
    constexpr uintptr_t AliveTeamNum = 0xBB8;
    constexpr uintptr_t bNoAliveHumanPlayer = 0xB44;

    // ---- [ الأسلحة والذخيرة وآلية الإطلاق ] ----
    constexpr uintptr_t CurrentWeapon = 0x2AF4;
    constexpr uintptr_t CurrentWeaponReplicated = 0x5E8;
    constexpr uintptr_t CurrentReloadWeapon = 0x3220;
    constexpr uintptr_t WeaponManagerComponent = 0x2628;
    constexpr uintptr_t ShootWeaponComponent = 0xFA0;
    constexpr uintptr_t ShootWeaponEntityComp = 0x1380;
    constexpr uintptr_t ShootWeaponEntity = 0x1380;
    constexpr uintptr_t ShootWeaponEffectComp = 0x1388;
    constexpr uintptr_t OwnerShootWeapon = 0x2D0;
    constexpr uintptr_t SwitchWeaponSpeedScale = 0x2C5C;
    constexpr uintptr_t CurrentStates = 0x1058;
    constexpr uintptr_t bIsWeaponFiring = 0x1840;
    constexpr uintptr_t bIsGunADS = 0x1134;

    // ---- [ خصائص السلاح والإطلاق والارتداد ] ----
    constexpr uintptr_t BulletFireSpeed = 0x560;
    constexpr uintptr_t BulletMomentum = 0x6D4;
    constexpr uintptr_t BulletRange = 0x6E4;
    constexpr uintptr_t BulletTrackDistance = 0x930;
    constexpr uintptr_t BaseImpactDamage = 0x6C4;
    constexpr uintptr_t ShootInterval = 0x5A0;
    constexpr uintptr_t ShootMode = 0x10F9;
    constexpr uintptr_t CurBulletNumInClip = 0x0;
    constexpr uintptr_t CurMaxBulletNumInOneClip = 0x1040;
    constexpr uintptr_t bHasSingleFireMode = 0x600;
    constexpr uintptr_t bHasAutoFireMode = 0x601;
    constexpr uintptr_t bHasBurstFireMode = 0x602;
    constexpr uintptr_t BurstShootInterval = 0x640;
    constexpr uintptr_t ReloadRate = 0x978;
    constexpr uintptr_t RecoilKickADS = 0xCF0;
    constexpr uintptr_t GameDeviationFactor = 0xC2C;
    constexpr uintptr_t GameDeviationAccuracy = 0xC30;

    // ---- [ إضافات السلاح / الملحقات ] ----
    constexpr uintptr_t AccessoriesVRecoilFactor = 0xBC8;
    constexpr uintptr_t AccessoriesHRecoilFactor = 0xBD0;
    constexpr uintptr_t AccessoriesRecoveryFactor = 0xBCC;
    constexpr uintptr_t AccessoriesDeviationFactor = 0xBF0;

    // ---- [ سلاح الشوتجن ] ----
    constexpr uintptr_t ShotGunCenterPerc = 0xC34;
    constexpr uintptr_t ShotGunVerticalSpread = 0xC38;
    constexpr uintptr_t ShotGunHorizontalSpread = 0xC3C;

    // ---- [ المركبات ] ----
    constexpr uintptr_t CurrentVehicle = 0xEB0;
    constexpr uintptr_t VehicleMovement = 0x1E50;
    constexpr uintptr_t VehicleCommon = 0xC08;
    constexpr uintptr_t VehicleDamageScale = 0x6D0;
    constexpr uintptr_t VehicleWeaponDeviationAngle = 0xC4C;
    constexpr uintptr_t lastForwardSpeed = 0xCD0;
    constexpr uintptr_t bIsEngineStarted = 0xBC8;
    constexpr uintptr_t ExtraBoostFactor = 0x20A4;
    constexpr uintptr_t bIsFPPOnVehicle = 0x17B4;

    // ---- [ مؤثرات الكاميرا والسكين ] ----
    constexpr uintptr_t CameraShakeInnerRadius = 0x2E8;
    constexpr uintptr_t CameraShakeOuterRadius = 0x234;
    constexpr uintptr_t CameraShakFalloff = 0x2F0;
    constexpr uintptr_t MinLOD = 0xA2C;

    // ---- [ عناصر أخرى ] ----
    constexpr uintptr_t CharacterOverrideAttrs = 0x1568;
    constexpr uintptr_t LaunchGravityScale = 0x5D8;
    constexpr uintptr_t HP = 0x354;
    constexpr uintptr_t HPMax = 0x350;
    constexpr uintptr_t Fuel = 0x43C;
    constexpr uintptr_t FuelMax = 0x438;
    constexpr uintptr_t PickUpDataList = 0x968;
    constexpr uintptr_t HitPerform = 0x618;
    constexpr uintptr_t bIsAirOpen = 0x728;
    constexpr uintptr_t AvatarComponent2 = 0x2FA0;
    constexpr uintptr_t SynData = 0x828;
    constexpr uintptr_t NetAvatarData = 0x440;
    constexpr uintptr_t ServerZoneId = 0x984;
}

#endif // OFFSETS_H
