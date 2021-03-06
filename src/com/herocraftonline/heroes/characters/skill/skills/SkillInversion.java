package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Setting;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class SkillInversion extends TargettedSkill {

    public SkillInversion(Heroes plugin) {
        super(plugin, "Inversion");
        setDescription("Percent damage equal to the (100% - target's mana) * $1");
        setUsage("/skill inversion");
        setArgumentRange(0, 1);
        setIdentifiers(new String[] { "skill inversion" });
        
        setTypes(SkillType.MANA, SkillType.DAMAGING, SkillType.SILENCABLE);
    }

    @Override
    public String getDescription(Hero hero) {
        double damageMod = SkillConfigManager.getUseSetting(hero, this, "damage-modifier", 1.0, false) +
                (SkillConfigManager.getUseSetting(hero, this, "damage-modifier-increase", 0.0, false) * hero.getSkillLevel(this));
        damageMod = damageMod > 0 ? damageMod : 0;
        String description = getDescription().replace("$1", damageMod + "");
        
        //COOLDOWN
        int cooldown = (SkillConfigManager.getUseSetting(hero, this, Setting.COOLDOWN.node(), 0, false)
                - SkillConfigManager.getUseSetting(hero, this, Setting.COOLDOWN_REDUCE.node(), 0, false) * hero.getSkillLevel(this)) / 1000;
        if (cooldown > 0) {
            description += " CD:" + cooldown + "s";
        }
        
        //MANA
        int mana = SkillConfigManager.getUseSetting(hero, this, Setting.MANA.node(), 10, false)
                - (SkillConfigManager.getUseSetting(hero, this, Setting.MANA_REDUCE.node(), 0, false) * hero.getSkillLevel(this));
        if (mana > 0) {
            description += " M:" + mana;
        }
        
        //HEALTH_COST
        int healthCost = SkillConfigManager.getUseSetting(hero, this, Setting.HEALTH_COST, 0, false) - 
                (SkillConfigManager.getUseSetting(hero, this, Setting.HEALTH_COST_REDUCE, mana, true) * hero.getSkillLevel(this));
        if (healthCost > 0) {
            description += " HP:" + healthCost;
        }
        
        //STAMINA
        int staminaCost = SkillConfigManager.getUseSetting(hero, this, Setting.STAMINA.node(), 0, false)
                - (SkillConfigManager.getUseSetting(hero, this, Setting.STAMINA_REDUCE.node(), 0, false) * hero.getSkillLevel(this));
        if (staminaCost > 0) {
            description += " FP:" + staminaCost;
        }
        
        //DELAY
        int delay = SkillConfigManager.getUseSetting(hero, this, Setting.DELAY.node(), 0, false) / 1000;
        if (delay > 0) {
            description += " W:" + delay + "s";
        }
        
        //EXP
        int exp = SkillConfigManager.getUseSetting(hero, this, Setting.EXP.node(), 0, false);
        if (exp > 0) {
            description += " XP:" + exp;
        }
        return description;
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("max-damage", 0);
        node.set("damage-modifier", 1);
        node.set("damage-modifier-increase", 0);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        if (!(target instanceof Player)) {
            return SkillResult.INVALID_TARGET;
        }
        Player player = hero.getPlayer();
        Hero enemy = plugin.getCharacterManager().getHero((Player) target);
        if (target.equals(player)) {
            return SkillResult.INVALID_TARGET;
        }
        int maxDamage = (int) SkillConfigManager.getUseSetting(hero, this, "max-damage", 0, false);
        double damageModifier = SkillConfigManager.getUseSetting(hero, this, "damage-modifier", 1.0, false) +
                (SkillConfigManager.getUseSetting(hero, this, "damage-modifier-increase", 0.0, false) * hero.getSkillLevel(this));
        damageModifier = damageModifier > 0 ? damageModifier : 0;
        int damage = (int) ((double) (((double) (100 - enemy.getMana()) / 100)) * enemy.getMaxHealth() * damageModifier);
        if (maxDamage != 0 && damage > maxDamage) {
            damage = maxDamage;
        }
        if (!damageCheck(player, target)) {
            return SkillResult.CANCELLED;
        }
        damageEntity(target, player, damage, DamageCause.MAGIC);
        //target.damage(damage, player);
        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

}