package peaacore.asm;

import java.util.List;

import moze_intel.projecte.gameObjs.blocks.CondenserMK2;
import moze_intel.projecte.gameObjs.blocks.MatterFurnace;
import moze_intel.projecte.gameObjs.blocks.Relay;
import moze_intel.projecte.gameObjs.entity.EntityLavaProjectile;
import moze_intel.projecte.utils.Constants;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.*;

import peaa.gameObjs.tiles.CondenserMK2TilePEAA;
import scala.actors.threadpool.Arrays;
import cpw.mods.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.FMLLaunchHandler;

public class PEAACoreTransformer implements IClassTransformer, Opcodes
{
	private static final String TARGETCLASSNAME = "moze_intel.projecte.gameObjs.ObjHandler";
	private static final String TARGETCLASSNAME2 = "moze_intel.projecte.gameObjs.tiles.DMFurnaceTile";
	private static final String TARGETCLASSNAME3 = "moze_intel.projecte.gameObjs.tiles.CondenserTile";
	// 新規追加
	private static final String TARGETCLASSNAME4 = "moze_intel.projecte.emc.EMCMapper";



	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass) {
		//if (!FMLLaunchHandler.side().isClient()) {return basicClass;}
		if(!TARGETCLASSNAME.equals(transformedName) && !TARGETCLASSNAME2.equals(transformedName)
				&& !TARGETCLASSNAME3.equals(transformedName) && !TARGETCLASSNAME4.equals(transformedName)) {return basicClass;}

		try {
			PEAACoreCorePlugin.logger.info("-------------------------Start PEAACore Transform--------------------------");
			ClassReader cr = new ClassReader(basicClass);
			ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
			cr.accept(new CustomVisitor(name, cw, transformedName), 8);
			basicClass = cw.toByteArray();
			PEAACoreCorePlugin.logger.info("-------------------------Finish PEAACore Transform-------------------------");
		} catch (Exception e) {
			throw new RuntimeException("failed : PEAACoreTransformer loading", e);
		}
		return basicClass;
	}

	class CustomVisitor extends ClassVisitor
	{
		String owner;
		String transformedName;
		public CustomVisitor(String owner, ClassVisitor cv, String transformedName)
		{
			super(Opcodes.ASM4, cv);
			this.owner = owner;
			this.transformedName = transformedName;
		}

		static final String targetMethodName = "<clinit>";				//ObjHandler
		static final String targetMethodName2 = "register";				//ObjHandler
		static final String targetMethodName3 = "<init>";				// DMFurnace TileEntity
		static final String targetMethodName4 = "getProgressScaled";	// CondenserTile
		// EMCMapper
		static final String targetMethodName5 = "addIMCRegistration";
		static final String targetMethodName6 = "loadEmcFromIMC";

		/**
		 * ここでどのメソッドかを判断してそれぞれの書き換え処理に飛ばしている
		 */
		@Override
		public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
			// ObjHandlerのフィールド書き換え
			if (targetMethodName.equals(FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(owner, name, desc))
					&& transformedName.equals(TARGETCLASSNAME)) {
				PEAACoreCorePlugin.logger.info("Start [clinit] Transform");
				return new CustomMethodVisitor(this.api, super.visitMethod(access, name, desc, signature, exceptions));

			}
			// ObjHandlerのregisterにTileEntityの追加
			if (targetMethodName2.equals(FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(owner, name, desc))
					&& transformedName.equals(TARGETCLASSNAME)) {
				PEAACoreCorePlugin.logger.info("Add TileEntity [Mk2PEAA]");
				PEAACoreCorePlugin.logger.info("Add Entity [EntityWarterProjectilePEAA]");
				PEAACoreCorePlugin.logger.info("Add TileEntity [RMFurnaceTilePEAA]");
				return new CustomMethodVisitor2(this.api, super.visitMethod(access, name, desc, signature, exceptions));

			}
			// DMかまどの extends するTileEntityを変更する処理其の2
			if (targetMethodName3.equals(FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(owner, name, desc))
					&& transformedName.equals(TARGETCLASSNAME2)) {
				PEAACoreCorePlugin.logger.info("Transform extends [RMFurnaceTilePEAA]");
				return new CustomMethodVisitor3(this.api, super.visitMethod(access, name, desc, signature, exceptions));

			}
			// コンデンサーのプログレスバー表示の修正
			if (targetMethodName4.equals(FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(owner, name, desc))
					&& transformedName.equals(TARGETCLASSNAME3)) {
				PEAACoreCorePlugin.logger.info("Transform extends [CondenserTile]");
				return new CustomMethodVisitor4(this.api, super.visitMethod(access, name, desc, signature, exceptions));

			}
			/**
			 * EMCMApper書き換え2つめ
			 *
			 * Mapを書き換えたので、それにあわせてメソッドも書き換え
			 */
			if (targetMethodName5.equals(FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(owner, name, desc))
					&& transformedName.equals(TARGETCLASSNAME4)) {
				PEAACoreCorePlugin.logger.info("Transform method [addIMCRegistration]");
				MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
				overrideAIMCR(mv);
				return super.visitMethod(access, name + "Old", desc, signature, exceptions);

			}
			if (targetMethodName6.equals(FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(owner, name, desc))
					&& transformedName.equals(TARGETCLASSNAME4)) {
				PEAACoreCorePlugin.logger.info("Transform method [loadEmcFromIMC]");
				MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
				overrideLEFIMC(mv);
				return super.visitMethod(access, name + "Old", desc, signature, exceptions);

			}

			return super.visitMethod(access, name, desc, signature, exceptions);
		}

		String DMTileName = "moze_intel/projecte/gameObjs/tiles/DMFurnaceTile";
		/**
		 * DMかまどの extends するTileEntityを変更する処理其の1
		 */
		@Override
		public void visit(int version, int access, String name, String signature,
	            String superName, String[] interfaces) {
			if (name.equals(DMTileName)) {
				super.visit(version, access, name, signature, "peaa/gameObjs/tiles/RMFurnaceTilePEAA", interfaces);
				return;
			}

			super.visit(version, access, name, signature, superName, interfaces);
		}


		/**
		 *  アイテムに振られるIDは準備が完了するまでに振りなおされることがあるようで、
		 *  最初にいきなりIDを取得＆登録してしまうと、後からIDを元に参照しようとした際には別のIDが割り振られてしまっていることが
		 *  あるようだ。そうなるとIDから正しいアイテムを取得することができず、EMC値の登録が出来ない。
		 *  なので最初はアイテムスタックを取得＆登録しておき、準備が完了しIDが固定されてから、アイテムスタックを元にIDを取得するように変更する。
		 *
		 *  そのための作業1つめ
		 *  	Mapの引数をSimpleStackからItemStackに変更。
		 */
		public FieldVisitor visitField(int access, String name, String desc,
	            String signature, Object value) {
			if (name.equals("IMCregistrations")) {
				return super.visitField(access, name, desc, "Ljava/util/LinkedHashMap<Lnet/minecraft/item/ItemStack;Ljava/lang/Integer;>;", value);
			}
			return super.visitField(access, name, desc, signature, value);
		}

		/**
		 * 強引なメソッド丸ごと書き換え
		 */
		private void overrideAIMCR(MethodVisitor mv)
		{
			mv.visitCode();
			Label l0 = new Label();
			mv.visitLabel(l0);
			mv.visitLineNumber(361, l0);
			mv.visitFieldInsn(GETSTATIC, "moze_intel/projecte/emc/EMCMapper", "IMCregistrations", "Ljava/util/LinkedHashMap;");
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/LinkedHashMap", "containsKey", "(Ljava/lang/Object;)Z", false);
			Label l1 = new Label();
			mv.visitJumpInsn(IFNE, l1);
			Label l2 = new Label();
			mv.visitLabel(l2);
			mv.visitLineNumber(363, l2);
			mv.visitFieldInsn(GETSTATIC, "moze_intel/projecte/emc/EMCMapper", "IMCregistrations", "Ljava/util/LinkedHashMap;");
			mv.visitVarInsn(ALOAD, 0);
			mv.visitVarInsn(ILOAD, 1);
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/LinkedHashMap", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false);
			mv.visitInsn(POP);
			Label l3 = new Label();
			mv.visitLabel(l3);
			mv.visitLineNumber(364, l3);
			mv.visitInsn(ICONST_1);
			mv.visitInsn(IRETURN);
			mv.visitLabel(l1);
			mv.visitLineNumber(367, l1);
			mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
			mv.visitInsn(ICONST_0);
			mv.visitInsn(IRETURN);
			Label l4 = new Label();
			mv.visitLabel(l4);
			mv.visitLocalVariable("stack", "Lnet/minecraft/item/ItemStack;", null, l0, l4, 0);
			mv.visitLocalVariable("value", "I", null, l0, l4, 1);
			mv.visitMaxs(3, 2);
			mv.visitEnd();
		}

		private void overrideLEFIMC(MethodVisitor mv) {
			mv.visitCode();
			Label l0 = new Label();
			mv.visitLabel(l0);
			mv.visitLineNumber(518, l0);
			mv.visitFieldInsn(GETSTATIC, "moze_intel/projecte/emc/EMCMapper", "IMCregistrations", "Ljava/util/LinkedHashMap;");
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/LinkedHashMap", "entrySet", "()Ljava/util/Set;", false);
			mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Set", "iterator", "()Ljava/util/Iterator;", true);
			mv.visitVarInsn(ASTORE, 2);
			Label l1 = new Label();
			mv.visitJumpInsn(GOTO, l1);
			Label l2 = new Label();
			mv.visitLabel(l2);
			mv.visitFrame(Opcodes.F_FULL, 3, new Object[] {Opcodes.TOP, Opcodes.TOP, "java/util/Iterator"}, 0, new Object[] {});
			mv.visitVarInsn(ALOAD, 2);
			mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "next", "()Ljava/lang/Object;", true);
			mv.visitTypeInsn(CHECKCAST, "java/util/Map$Entry");
			mv.visitVarInsn(ASTORE, 1);
			Label l3 = new Label();
			mv.visitLabel(l3);
			mv.visitLineNumber(520, l3);
			mv.visitTypeInsn(NEW, "moze_intel/projecte/emc/SimpleStack");
			mv.visitInsn(DUP);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map$Entry", "getKey", "()Ljava/lang/Object;", true);
			mv.visitTypeInsn(CHECKCAST, "net/minecraft/item/ItemStack");
			mv.visitMethodInsn(INVOKESPECIAL, "moze_intel/projecte/emc/SimpleStack", "<init>", "(Lnet/minecraft/item/ItemStack;)V", false);
			mv.visitVarInsn(ASTORE, 0);
			Label l4 = new Label();
			mv.visitLabel(l4);
			mv.visitLineNumber(521, l4);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map$Entry", "getValue", "()Ljava/lang/Object;", true);
			mv.visitTypeInsn(CHECKCAST, "java/lang/Integer");
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
			Label l5 = new Label();
			mv.visitJumpInsn(IFGT, l5);
			Label l6 = new Label();
			mv.visitLabel(l6);
			mv.visitLineNumber(523, l6);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKESTATIC, "moze_intel/projecte/emc/EMCMapper", "addToBlacklist", "(Lmoze_intel/projecte/emc/SimpleStack;)V", false);
			Label l7 = new Label();
			mv.visitLabel(l7);
			mv.visitLineNumber(524, l7);
			mv.visitJumpInsn(GOTO, l1);
			mv.visitLabel(l5);
			mv.visitLineNumber(527, l5);
			mv.visitFrame(Opcodes.F_FULL, 3, new Object[] {"moze_intel/projecte/emc/SimpleStack", "java/util/Map$Entry", "java/util/Iterator"}, 0, new Object[] {});
			mv.visitVarInsn(ALOAD, 0);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map$Entry", "getValue", "()Ljava/lang/Object;", true);
			mv.visitTypeInsn(CHECKCAST, "java/lang/Integer");
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
			mv.visitMethodInsn(INVOKESTATIC, "moze_intel/projecte/emc/EMCMapper", "addMapping", "(Lmoze_intel/projecte/emc/SimpleStack;I)V", false);
			mv.visitLabel(l1);
			mv.visitLineNumber(518, l1);
			mv.visitFrame(Opcodes.F_FULL, 3, new Object[] {Opcodes.TOP, Opcodes.TOP, "java/util/Iterator"}, 0, new Object[] {});
			mv.visitVarInsn(ALOAD, 2);
			mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "hasNext", "()Z", true);
			mv.visitJumpInsn(IFNE, l2);
			Label l8 = new Label();
			mv.visitLabel(l8);
			mv.visitLineNumber(531, l8);
			mv.visitInsn(RETURN);
			mv.visitLocalVariable("sStack", "Lmoze_intel/projecte/emc/SimpleStack;", null, l4, l1, 0);
			mv.visitLocalVariable("entry", "Ljava/util/Map$Entry;", "Ljava/util/Map$Entry<Lnet/minecraft/item/ItemStack;Ljava/lang/Integer;>;", l3, l1, 1);
			mv.visitMaxs(3, 3);
			mv.visitEnd();
		}

	}

	/**
	 * [clinit]
	 * ObjHanlderの
	 * public static Block condenserMk2 = new CondenserMK2();
	 * を
	 * public static Block condenserMk2 = new CondenserMK2PEAA();
	 * に変更
	 *
	 * MatterFurnace から MatterFurnacePEAA
	 *
	 */
	class CustomMethodVisitor extends MethodVisitor {
		static final String targetFieldName = "condenserMk2";
		static final String targetFieldName2 = "rmFurnaceOff";
		static final String targetFieldName3 = "rmFurnaceOn";
		static final String targetFieldName4 = "everTide";

		public CustomMethodVisitor(int api, MethodVisitor mv) {
            super(api, mv);
        }

		@Override
		public void visitFieldInsn(int opcode, String owner, String name, String desc) {
			if (targetFieldName.equals(name)) {
				PEAACoreCorePlugin.logger.info("Start [condenserMk2] Transform");
				mv.visitTypeInsn(NEW, "peaa/gameObjs/blocks/CondenserMK2PEAA");
				mv.visitInsn(DUP);
				mv.visitMethodInsn(INVOKESPECIAL, "peaa/gameObjs/blocks/CondenserMK2PEAA", "<init>", "()V", false);

			}
			if (targetFieldName2.equals(name)) {
				PEAACoreCorePlugin.logger.info("Start [MatterFurnace_Off] Transform");
				mv.visitTypeInsn(NEW, "peaa/gameObjs/blocks/MatterFurnacePEAA");
				mv.visitInsn(DUP);
				mv.visitInsn(ICONST_0);
				mv.visitInsn(ICONST_1);
				mv.visitMethodInsn(INVOKESPECIAL, "peaa/gameObjs/blocks/MatterFurnacePEAA", "<init>", "(ZZ)V", false);

			}
			// RMTilePEAAに変更
			if (targetFieldName3.equals(name)) {
				PEAACoreCorePlugin.logger.info("Start [MatterFurnace_On] Transform");
				mv.visitTypeInsn(NEW, "peaa/gameObjs/blocks/MatterFurnacePEAA");
				mv.visitInsn(DUP);
				mv.visitInsn(ICONST_1);
				mv.visitInsn(ICONST_1);
				mv.visitMethodInsn(INVOKESPECIAL, "peaa/gameObjs/blocks/MatterFurnacePEAA", "<init>", "(ZZ)V", false);

			}
			// EvertideAmuletPEAAに変更
			if (targetFieldName4.equals(name)) {
				PEAACoreCorePlugin.logger.info("Start [EvertideAmulet] Transform");
				mv.visitTypeInsn(NEW, "peaa/gameObjs/items/EvertideAmuletPEAA");
				mv.visitInsn(DUP);
				mv.visitMethodInsn(INVOKESPECIAL, "peaa/gameObjs/items/EvertideAmuletPEAA", "<init>", "()V", false);

			}

			super.visitFieldInsn(opcode, owner, name, desc);
		}
	}

	/**
	 * [register]
	 * ObjHandlerに
	 * 		GameRegistry.registerTileEntity(CondenserMK2TilePEAA.class, "CondenserMK2 Tile  PEAA");
	 * を追加
	 *
	 * 変更PEAAにする
	 * mv.visitLdcInsn(Type.getType("Lpeaa/gameObjs/entity/EntityWaterProjectilePEAA;"));
	 */
	class CustomMethodVisitor2 extends MethodVisitor {
		public CustomMethodVisitor2(int api, MethodVisitor mv) {
            super(api, mv);
        }

		@Override
		public void visitCode() {
			super.visitCode();
			// ここから追加処理
			// add MK2Tile
			mv.visitLdcInsn(Type.getType("Lpeaa/gameObjs/tiles/CondenserMK2TilePEAA;"));
			mv.visitLdcInsn("CondenserMK2 Tile  PEAA");
			mv.visitMethodInsn(INVOKESTATIC, "cpw/mods/fml/common/registry/GameRegistry", "registerTileEntity", "(Ljava/lang/Class;Ljava/lang/String;)V", false);

			// add RMFurnaceTilePEAA
			mv.visitLdcInsn(Type.getType("Lpeaa/gameObjs/tiles/RMFurnaceTilePEAA;"));
			mv.visitLdcInsn("RM Furnace Tile PEAA");
			mv.visitMethodInsn(INVOKESTATIC, "cpw/mods/fml/common/registry/GameRegistry", "registerTileEntity", "(Ljava/lang/Class;Ljava/lang/String;)V", false);

			// add EntityWaterProjectilePEAA
			mv.visitLdcInsn(Type.getType("Lpeaa/gameObjs/entity/EntityWaterProjectilePEAA;"));
			mv.visitLdcInsn("Water Water PEAA");
			mv.visitIntInsn(BIPUSH, 10);
			mv.visitFieldInsn(GETSTATIC, "moze_intel/projecte/PECore", "instance", "Lmoze_intel/projecte/PECore;");
			mv.visitIntInsn(SIPUSH, 256);
			mv.visitIntInsn(BIPUSH, 10);
			mv.visitInsn(ICONST_1);
			mv.visitMethodInsn(INVOKESTATIC, "cpw/mods/fml/common/registry/EntityRegistry", "registerModEntity", "(Ljava/lang/Class;Ljava/lang/String;ILjava/lang/Object;IIZ)V", false);
		}

		@Override
		public void visitLdcInsn(Object cst) {
			if (Type.getType("Lmoze_intel/projecte/gameObjs/entity/EntityWaterProjectile;").equals(cst)) {
				mv.visitLdcInsn(Type.getType("Lpeaa/gameObjs/entity/EntityWaterProjectilePEAA;"));
				return;
			}
			super.visitLdcInsn(cst);
		}
	}

	/**
	 * mv.visitMethodInsn(INVOKESPECIAL, "moze_intel/projecte/gameObjs/tiles/RMFurnaceTile", "<init>", "()V", false);
	 * マターかまどの変更処理
	 */
	class CustomMethodVisitor3 extends MethodVisitor {
		static final String targetVisitMethodInsnName = "moze_intel/projecte/gameObjs/tiles/RMFurnaceTile";

		public CustomMethodVisitor3(int api, MethodVisitor mv) {
            super(api, mv);
        }

		@Override
		public void visitMethodInsn(int opcode, String owner, String name,
	            String desc, boolean itf) {
			if (owner.equals(targetVisitMethodInsnName)) {
				super.visitMethodInsn(opcode, "peaa/gameObjs/tiles/RMFurnaceTilePEAA", name, desc, itf);
				return;
			}
			super.visitMethodInsn(opcode, owner, name, desc, itf);

		}
	}

	/**
	 * コンデンサーのプログレスバーの表示が上手くいかない問題の修正
	 * とはいってもLongのキャストを挟むだけ(数が22億くらいを超えてintの上限を突破しているのが原因なので)
	 *
	 * return (displayEmc * Constants.MAX_CONDENSER_PROGRESS) / requiredEmc;
	 * から
	 * return (int)( ((long)displayEmc * Constants.MAX_CONDENSER_PROGRESS) / requiredEmc);
	 * にするだけ
	 */
	class CustomMethodVisitor4 extends MethodVisitor {
		static final String targetVisitMethodInsnName = "moze_intel/projecte/gameObjs/tiles/RMFurnaceTile";
		int count = 0;

		public CustomMethodVisitor4(int api, MethodVisitor mv) {
            super(api, mv);
        }

		/**
		 * 書き換えたい visitInsnは3つめ
		 */
		public void visitInsn(int opcode) {
			if (opcode == IRETURN)
				count++;

			if (count == 3) {
				mv.visitVarInsn(ALOAD, 0);
				mv.visitFieldInsn(GETFIELD, "moze_intel/projecte/gameObjs/tiles/CondenserTile", "displayEmc", "I");
				mv.visitInsn(I2L);
				mv.visitLdcInsn(new Long(102L));
				mv.visitInsn(LMUL);
				mv.visitVarInsn(ALOAD, 0);
				mv.visitFieldInsn(GETFIELD, "moze_intel/projecte/gameObjs/tiles/CondenserTile", "requiredEmc", "I");
				mv.visitInsn(I2L);
				mv.visitInsn(LDIV);
				mv.visitInsn(L2I);
			}
			super.visitInsn(opcode);
		}
	}
}
