package com.name.realplayer.transformers;

import com.name.realplayer.Config;
import com.name.realplayer.RealPlayer;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.EnumSet;

public class LaunchPluginService implements ILaunchPluginService {
    private static final EnumSet<Phase> YAY = EnumSet.of(Phase.AFTER);
    private static final EnumSet<Phase> NAY = EnumSet.noneOf(Phase.class);

    @Override
    public String name() {
        return "realplayer_launchpluginservice";
    }

    @Override
    public EnumSet<Phase> handlesClass(Type type, boolean b) {
        String name = type.getInternalName();
        return (name.startsWith("com/name/realplayer") || name.startsWith("net/minecraft/") || name.startsWith("net/minecraftforge/") || name.startsWith("net/neoforged/")) ? NAY : YAY; // handle anything thats not us/forge/vanilla
    }

    //this should support all cases of Forge and NeoForge where ITransformationService even exists lmao
    // tested on: 1.16.5 Forge, 1.19.2 Forge, 1.21.1 NeoForge
    @Override
    public int processClassWithFlags(Phase phase, ClassNode classNode, Type classType, String reason) {
        if (!Config.isPatchingEnabledForClassAndMethod(classNode.name, "0-all-enabled")) return ComputeFlags.NO_REWRITE;
        
        boolean rewrite = false;
        
        for (MethodNode method : classNode.methods) {
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction.getOpcode() == Opcodes.INSTANCEOF && instruction instanceof TypeInsnNode && (((TypeInsnNode) instruction).desc.equals("net/minecraftforge/common/util/FakePlayer") || ((TypeInsnNode) instruction).desc.equals("net/neoforged/neoforge/common/util/FakePlayer"))) {
                    if (!Config.isPatchingEnabledForClassAndMethod(classNode.name, method.name)) {
                        RealPlayer.LOGGER.info("Skipping FakePlayer check in class {} method {}: disabled in config", classNode.name, method.name);
                        continue;
                    }
                    RealPlayer.LOGGER.info("Disabling FakePlayer check in class {} method {}", classNode.name, method.name);
                    ((TypeInsnNode) instruction).desc = "java/lang/Boolean"; // Boolean can't be extended so this should always return false. I am using a java class here because using com/name/realplayer/core/DummyClass doesn't work sometimes
                    rewrite = true;
                } else if (instruction.getOpcode() == Opcodes.INVOKESTATIC && instruction instanceof MethodInsnNode && ((MethodInsnNode) instruction).name.equals("isFake") && ((MethodInsnNode) instruction).desc.contains("Player") && ((MethodInsnNode) instruction).desc.endsWith("Z") && ((MethodInsnNode) instruction).owner.equals("dev/architectury/hooks/level/entity/PlayerHooks")) {
                    if (!Config.isPatchingEnabledForClassAndMethod(classNode.name, method.name)) {
                        RealPlayer.LOGGER.info("Skipping Architectury FakePlayer check in class {} method {}: disabled in config", classNode.name, method.name);
                        continue;
                    }
                    RealPlayer.LOGGER.info("Disabling Architectury FakePlayer check in class {} method {}", classNode.name, method.name);
                    method.instructions.insertBefore(instruction, new MethodInsnNode(Opcodes.INVOKESTATIC, "com/name/realplayer/core/StaticHelpers", "neverFake", "(Ljava/lang/Object;)Z", false));
                    method.instructions.remove(instruction);
                    rewrite = true;
                }
            }
        }
        
        return rewrite ? ComputeFlags.SIMPLE_REWRITE : ComputeFlags.NO_REWRITE;
    }
}
