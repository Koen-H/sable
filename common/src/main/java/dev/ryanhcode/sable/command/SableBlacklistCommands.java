package dev.ryanhcode.sable.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.ryanhcode.sable.config.SubLevelBlacklistConfig;
import dev.ryanhcode.sable.index.SableTags;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class SableBlacklistCommands {

    public static void register(final LiteralArgumentBuilder<CommandSourceStack> sableBuilder, final CommandBuildContext buildContext) {
        sableBuilder.then(Commands.literal("blacklist")
                .executes(ctx -> {
                    final CommandSourceStack source = ctx.getSource();
                    final String configPath = SubLevelBlacklistConfig.getConfigPath().toString();

                    source.sendSuccess(() -> Component.translatable("commands.sable.blacklist.header")
                            .withStyle(ChatFormatting.GOLD), false);

                    // Config file entries
                    source.sendSuccess(() -> Component.translatable("commands.sable.blacklist.config_section", configPath)
                            .withStyle(Style.EMPTY
                                    .withColor(ChatFormatting.YELLOW)
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, configPath))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                            Component.translatable("commands.sable.blacklist.open_file")))), false);

                    final Set<ResourceLocation> configEntries = SubLevelBlacklistConfig.getBlacklisted();
                    if (configEntries.isEmpty()) {
                        source.sendSuccess(() -> Component.translatable("commands.sable.blacklist.empty")
                                .withStyle(ChatFormatting.GRAY), false);
                    } else {
                        final List<ResourceLocation> sorted = new ArrayList<>(configEntries);
                        sorted.sort(Comparator.comparing(ResourceLocation::toString));
                        for (final ResourceLocation id : sorted) {
                            source.sendSuccess(() -> Component.literal("  • ")
                                    .append(Component.literal(id.toString()).withStyle(ChatFormatting.WHITE)), false);
                        }
                    }

                    // Tag entries
                    source.sendSuccess(() -> Component.translatable("commands.sable.blacklist.tag_section")
                            .withStyle(ChatFormatting.YELLOW), false);

                    final Optional<List<Holder<Block>>> tagHolders = source.getServer().registryAccess()
                            .lookupOrThrow(Registries.BLOCK)
                            .getTag(SableTags.SUBLEVEL_BLACKLISTED)
                            .map(tag -> tag.stream().toList());

                    if (tagHolders.isEmpty() || tagHolders.get().isEmpty()) {
                        source.sendSuccess(() -> Component.translatable("commands.sable.blacklist.empty")
                                .withStyle(ChatFormatting.GRAY), false);
                    } else {
                        final List<ResourceLocation> tagIds = tagHolders.get().stream()
                                .map(h -> h.unwrapKey().map(k -> k.location()).orElse(null))
                                .filter(id -> id != null)
                                .sorted(Comparator.comparing(ResourceLocation::toString))
                                .toList();
                        for (final ResourceLocation id : tagIds) {
                            source.sendSuccess(() -> Component.literal("  • ")
                                    .append(Component.literal(id.toString()).withStyle(ChatFormatting.WHITE)), false);
                        }
                    }

                    return 1;
                })
        );
    }
}
