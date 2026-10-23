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
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Электрощиток 16x16 (глубина 8). Работает как рычаг (источник редстоун-сигнала), но:
 * рычажки автоматов спрятаны за дверцей — сначала дверцу надо открыть.
 * ПКМ по верхней левой дверце: закрыта -> открывает; открыта -> клик по автоматам включает/выключает
 * питание, клик по остальной части проёма (или Shift+ПКМ) закрывает дверцу.
 * Остальные части щитка (правая дверь, счётчики) для взаимодействия недоступны.
 */
public class ElectricalPanelBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<ElectricalPanelBlock> CODEC = simpleCodec(ElectricalPanelBlock::new);
    /** Дверца открыта. */
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    /** Питание включено (как у рычага). */
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    // Корпус занимает заднюю половину клетки (глубина 8, как основание рычага)
    private static final VoxelShape SHAPE_NORTH = Block.box(0, 0, 8, 16, 16, 16);
    private static final VoxelShape SHAPE_SOUTH = Block.box(0, 0, 0, 16, 16, 8);
    private static final VoxelShape SHAPE_EAST = Block.box(0, 0, 0, 8, 16, 16);
    private static final VoxelShape SHAPE_WEST = Block.box(8, 0, 0, 16, 16, 16);

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
        return switch (state.getValue(FACING)) {
            case SOUTH -> SHAPE_SOUTH;
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
            default -> SHAPE_NORTH;
        };
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

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        Direction facing = state.getValue(FACING);
        if (hit.getDirection() != facing) {
            return InteractionResult.PASS;
        }
        double[] uv = frontUv(facing, hit.getLocation(), pos);
        double u = uv[0], v = uv[1];
        // верхняя левая дверца: u 0..8, v 0..7; остальное — недоступно
        if (u < 0.0 || u > 8.0 || v < 0.0 || v > 7.0) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!state.getValue(OPEN)) {
            setOpen(level, pos, state, true);                         // закрыто — открываем (автоматы недоступны)
        } else if (player.isShiftKeyDown()) {
            setOpen(level, pos, state, false);
        } else if (u >= 0.8 && u <= 6.8 && v >= 1.5 && v <= 6.0) {
            toggleBreaker(level, pos, state);                         // клик по автоматам — как рычаг
        } else {
            setOpen(level, pos, state, false);                        // клик по остальному проёму — закрыть
        }
        return InteractionResult.SUCCESS;
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
