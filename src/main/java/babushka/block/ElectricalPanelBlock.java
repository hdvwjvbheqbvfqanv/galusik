package babushka.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Электрощиток 16x16 (глубина 8). Работает как рычаг (источник редстоун-сигнала), но автоматы спрятаны за дверцей.
 * Всё срабатывает ТОЛЬКО при наведении курсора на нужную часть:
 *  - верхняя левая дверца (закрыта): ПКМ по ней — открывается; открытую можно закрыть ПКМ по самой створке
 *    или Shift+ПКМ по месту дверцы;
 *  - открытая: ПКМ по маленькому жёлтому рычажку ВТОРОГО СЛЕВА автомата — переключает питание (рычажок опускается/поднимается);
 *    остальные три жёлтых рычажка — декор;
 *  - все остальные части щитка (правая дверь, счётчики, корпус) ни на что не влияют.
 */
public class ElectricalPanelBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<ElectricalPanelBlock> CODEC = simpleCodec(ElectricalPanelBlock::new);
    /** Дверца открыта. */
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    /** Питание включено (как у рычага). */
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    // Корпус занимает заднюю половину клетки (глубина 8, как основание рычага); лицо смотрит на север при facing=north.
    // Открытая дверца (петли слева для игрока) торчит вперёд: box(15.2, 9.2, 0, 16, 16, 9).
    private static final VoxelShape[] BODY = new VoxelShape[4];
    private static final VoxelShape[] PLATE = new VoxelShape[4];
    static {
        for (int turns = 0; turns < 4; turns++) {
            BODY[turns] = rotatedBox(turns, 0, 0, 8, 16, 16, 16);
            PLATE[turns] = rotatedBox(turns, 15.2, 9.2, 0, 16, 16, 9);
        }
    }

    /** Коробка в координатах "лицом на север", повёрнутая на turns*90° по часовой (север -> восток -> юг -> запад). */
    private static VoxelShape rotatedBox(int turns, double x0, double y0, double z0, double x1, double y1, double z1) {
        double ax = x0, az = z0, bx = x1, bz = z1;
        for (int i = 0; i < turns; i++) {
            double nx0 = 16 - bz, nx1 = 16 - az, nz0 = ax, nz1 = bx;
            ax = nx0; bx = nx1; az = nz0; bz = nz1;
        }
        return Block.box(Math.min(ax, bx), y0, Math.min(az, bz), Math.max(ax, bx), y1, Math.max(az, bz));
    }

    private static int turns(Direction facing) {
        return switch (facing) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
    }

    public ElectricalPanelBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(OPEN, false)
            .setValue(POWERED, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN, POWERED);
    }

    /** Ставится на стену (лицом от стены), на пол/потолок — лицом к игроку. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        Direction facing = face.getAxis().isHorizontal() ? face : context.getHorizontalDirection().getOpposite();
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int t = turns(state.getValue(FACING));
        return state.getValue(OPEN) ? Shapes.or(BODY[t], PLATE[t]) : BODY[t];
    }

    // ---------------------------------------------------------------- как у рычага: источник сигнала

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(POWERED) ? 15 : 0;
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(POWERED) && state.getValue(FACING) == direction ? 15 : 0;
    }

    // ---------------------------------------------------------------- взаимодействие

    /** Координаты клика на лицевой стороне в "пикселях" (0..16, u слева направо, v сверху вниз). */
    private static double[] frontUv(Direction facing, Vec3 hit, BlockPos pos) {
        double x = hit.x - pos.getX();
        double y = hit.y - pos.getY();
        double z = hit.z - pos.getZ();
        double u = switch (facing) {
            case NORTH -> 1.0 - x;
            case SOUTH -> x;
            case EAST -> 1.0 - z;
            default -> z;
        };
        return new double[] {u * 16.0, (1.0 - y) * 16.0};
    }

    /** Верхняя левая дверца на лицевой стороне: u 0..9, v 0..6.8. */
    private static boolean onDoor(double u, double v) {
        return u >= 0.0 && u <= 9.0 && v >= 0.0 && v <= 6.8;
    }

    /**
     * Маленький жёлтый рычажок второго слева автомата (u 2.85..3.75). Положение зависит от питания:
     * включено — вверху (v 2.8..4.0), выключено — внизу (v 4.4..5.6). Небольшой запас на перспективу.
     */
    private static boolean onHandle(double u, double v, boolean powered) {
        double v0 = powered ? 2.8 : 4.4;
        double v1 = powered ? 4.0 : 5.6;
        return u >= 2.85 - 0.4 && u <= 3.75 + 0.4 && v >= v0 - 0.35 && v <= v1 + 0.35;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        Direction facing = state.getValue(FACING);
        boolean open = state.getValue(OPEN);
        Vec3 loc = hit.getLocation();
        double lx = loc.x - pos.getX(), ly = loc.y - pos.getY(), lz = loc.z - pos.getZ();

        // открытая створка: ПКМ по ней закрывает дверцу
        if (open && PLATE[turns(facing)].bounds().inflate(1.0E-3).contains(lx, ly, lz)) {
            if (!level.isClientSide()) {
                setOpen(level, pos, state, false);
            }
            return InteractionResult.SUCCESS;
        }

        // всё остальное — только с лицевой стороны
        if (hit.getDirection() != facing) {
            return InteractionResult.PASS;
        }
        double[] uv = frontUv(facing, loc, pos);
        double u = uv[0], v = uv[1];

        if (!open) {
            if (!onDoor(u, v)) {
                return InteractionResult.PASS;               // правая дверь, счётчики и корпус ничего не делают
            }
            if (!level.isClientSide()) {
                setOpen(level, pos, state, true);
            }
            return InteractionResult.SUCCESS;
        }

        if (onDoor(u, v) && player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                setOpen(level, pos, state, false);
            }
            return InteractionResult.SUCCESS;
        }
        if (onHandle(u, v, state.getValue(POWERED))) {
            if (!level.isClientSide()) {
                toggleBreaker(level, pos, state);            // работает как рычаг
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;                       // мимо рычажка — ничего не происходит
    }

    // ---------------------------------------------------------------- действия (их же использует бабушка)

    public static void setOpen(Level level, BlockPos pos, BlockState state, boolean open) {
        if (state.getValue(OPEN) == open) {
            return;
        }
        level.setBlock(pos, state.setValue(OPEN, open), 3);
        level.playSound(null, pos, open ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE,
            SoundSource.BLOCKS, 0.5F, 1.2F);
    }

    /** Переключает питание (как рычаг). */
    public static void toggleBreaker(Level level, BlockPos pos, BlockState state) {
        boolean on = !state.getValue(POWERED);
        level.setBlock(pos, state.setValue(POWERED, on), 3);
        level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.4F, on ? 0.6F : 0.5F);
        Block block = state.getBlock();
        level.updateNeighborsAt(pos, block);
        level.updateNeighborsAt(pos.relative(state.getValue(FACING).getOpposite()), block);   // блок, к которому прикреплён
    }

    /** Для бабушки: открывает дверцу (если закрыта) и переключает питание. */
    public static void toggleByMob(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ElectricalPanelBlock)) {
            return;
        }
        if (!state.getValue(OPEN)) {
            setOpen(level, pos, state, true);
            state = level.getBlockState(pos);
        }
        toggleBreaker(level, pos, state);
    }

    public static void closeByMob(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof ElectricalPanelBlock) {
            setOpen(level, pos, state, false);
        }
    }

    /** Когда включённый щиток убирают, соседям нужно обновиться (как у рычага). */
    protected void affectNeighborsAfterRemoval(BlockState state, net.minecraft.server.level.ServerLevel level,
                                               BlockPos pos, boolean movedByPiston) {
        if (!movedByPiston && state.getValue(POWERED)) {
            Block block = state.getBlock();
            level.updateNeighborsAt(pos, block);
            level.updateNeighborsAt(pos.relative(state.getValue(FACING).getOpposite()), block);
        }
    }
}
