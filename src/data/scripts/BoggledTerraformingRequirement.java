package data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketSpecAPI;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import data.campaign.econ.boggledTools;
import data.campaign.econ.industries.BoggledIndustryInterface;

import java.util.List;
import java.util.Map;

import static data.campaign.econ.industries.BoggledCommonIndustry.getFocusMarketOrMarket;

public class BoggledTerraformingRequirement {
    public static class RequirementContext {
        private final BaseIndustry industry;
        private final BoggledIndustryInterface industryInterface;
        private final MarketAPI market;
        private final PlanetAPI planet;
        private final StarSystemAPI starSystem;
        private final CampaignFleetAPI fleet;

        public RequirementContext(BaseIndustry industry) {
            this.industry = industry;
            this.industryInterface = (industry instanceof BoggledIndustryInterface) ? (BoggledIndustryInterface) industry : null;
            this.market = industry.getMarket();
            this.planet = this.market.getPlanetEntity();
            this.starSystem = this.planet.getStarSystem();
            this.fleet = Global.getSector().getPlayerFleet();
        }

        public RequirementContext(MarketAPI market) {
            this.industry = null;
            this.industryInterface = null;
            this.market = market;
            this.planet = market.getPlanetEntity();
            this.starSystem = this.planet.getStarSystem();
            this.fleet = Global.getSector().getPlayerFleet();
        }

        public RequirementContext(CampaignFleetAPI fleet) {
            this.industry = null;
            this.industryInterface = null;
            this.fleet = fleet;
            this.planet = boggledTools.getClosestPlanetToken(fleet);
            this.starSystem = this.fleet.getStarSystem();
            if (this.planet != null) {
                this.market = this.planet.getMarket();
            } else {
                this.market = null;
            }
        }

        public RequirementContext getFocusContext() {
            return new RequirementContext(getFocusMarketOrMarket(this.getMarket()));
        }

        public BaseIndustry getIndustry() { return industry; }
        public BoggledIndustryInterface getIndustryInterface() { return industryInterface; }
        public MarketAPI getMarket() { return market; }
        public PlanetAPI getPlanet() { return planet; }
        public StarSystemAPI getStarSystem() { return starSystem; }
        public CampaignFleetAPI getFleet() { return fleet; }
    }

    public abstract static class TerraformingRequirement {
        private final String requirementId;
        private final boolean invert;

        public String getRequirementId() { return requirementId; }

        protected TerraformingRequirement(String requirementId, boolean invert) {
            this.requirementId = requirementId;
            this.invert = invert;
        }

        public void addTokenReplacements(Map<String, String> tokenReplacements) {}

        protected abstract boolean checkRequirementImpl(RequirementContext ctx);

        public final boolean checkRequirement(RequirementContext ctx) {
            boolean ret = checkRequirementImpl(ctx);
            if (invert) {
                ret = !ret;
            }
            return ret;
        }
    }

    public static class AlwaysTrue extends TerraformingRequirement {
        public AlwaysTrue(String requirementId, boolean invert) {
            super(requirementId, invert);
        }

        @Override
        protected boolean checkRequirementImpl(RequirementContext ctx) {
            return true;
        }
    }

    public static class PlanetType extends TerraformingRequirement {
        String planetTypeId;

        public PlanetType(String requirementId, boolean invert, String planetTypeId) {
            super(requirementId, invert);
            this.planetTypeId = planetTypeId;
        }

        @Override
        protected boolean checkRequirementImpl(RequirementContext ctx) {
            return planetTypeId.equals(boggledTools.getPlanetType(ctx.getPlanet()).getPlanetId());
        }
    }

    public static class FocusPlanetType extends PlanetType {
        public FocusPlanetType(String requirementId, boolean invert, String planetTypeId) {
            super(requirementId, invert, planetTypeId);
        }

        @Override
        protected boolean checkRequirementImpl(RequirementContext ctx) {
            if (ctx.getMarket().getPrimaryEntity().getOrbitFocus().getMarket() == null) {
                return false;
            }
            return super.checkRequirementImpl(new RequirementContext(ctx.getMarket().getPrimaryEntity().getOrbitFocus().getMarket()));
        }
    }

    public static class MarketHasCondition extends TerraformingRequirement {
        String conditionId;
        public MarketHasCondition(String requirementId, boolean invert, String conditionId) {
            super(requirementId, invert);
            this.conditionId = conditionId;
        }

        @Override
        protected boolean checkRequirementImpl(RequirementContext ctx) {
            return ctx.getMarket().hasCondition(conditionId);
        }
    }

    public static class FocusMarketHasCondition extends MarketHasCondition {
        public FocusMarketHasCondition(String requirementId, boolean invert, String conditionId) {
            super(requirementId, invert, conditionId);
        }

        @Override
        protected boolean checkRequirementImpl(RequirementContext ctx) {
            if (ctx.getMarket().getPrimaryEntity().getOrbitFocus().getMarket() == null) {
                return false;
            }
            return super.checkRequirementImpl(new RequirementContext(ctx.getMarket().getPrimaryEntity().getOrbitFocus().getMarket()));
        }
    }

    public static class MarketHasIndustry extends TerraformingRequirement {
        String industryId;
        public MarketHasIndustry(String requirementId, boolean invert, String industryId) {
            super(requirementId, invert);
            this.industryId = industryId;
        }

        @Override
        public void addTokenReplacements(Map<String, String> tokenReplacements) {
            tokenReplacements.put("$industry", Global.getSettings().getIndustrySpec(industryId).getName());
        }

        @Override
        protected boolean checkRequirementImpl(RequirementContext ctx) {
            Industry industry = ctx.getMarket().getIndustry(industryId);
            return industry != null && industry.isFunctional() && ctx.getMarket().hasIndustry(industryId);
        }
    }

    public static class MarketHasIndustryWithItem extends MarketHasIndustry {
        String itemId;
        public MarketHasIndustryWithItem(String requirementId, boolean invert, String industryId, String itemId) {
            super(requirementId, invert, industryId);
            this.itemId = itemId;
        }

        @Override
        public void addTokenReplacements(Map<String, String> tokenReplacements) {
            super.addTokenReplacements(tokenReplacements);
            tokenReplacements.put("$item", Global.getSettings().getSpecialItemSpec(itemId).getName());
        }

        @Override
        protected boolean checkRequirementImpl(RequirementContext ctx) {
            if (!super.checkRequirementImpl(ctx)) {
                return false;
            }
            Industry industry = ctx.getMarket().getIndustry(industryId);
            for (SpecialItemData specialItemData : industry.getVisibleInstalledItems()) {
                if (itemId.equals(specialItemData.getId())) {
                    return true;
                }
            }
            return false;
        }
    }

    public static class MarketHasIndustryWithAICore extends MarketHasIndustry {
        String aiCoreId;
        public MarketHasIndustryWithAICore(String requirementId, boolean invert, String industryId, String aiCoreId) {
            super(requirementId, invert, industryId);
            this.aiCoreId = aiCoreId;
        }

        @Override
        public void addTokenReplacements(Map<String, String> tokenReplacements) {
            super.addTokenReplacements(tokenReplacements);
            tokenReplacements.put("$aiCore", Global.getSettings().getCommoditySpec(aiCoreId).getName());
        }

        @Override
        protected boolean checkRequirementImpl(RequirementContext ctx) {
            if (!super.checkRequirementImpl(ctx)) {
                return false;
            }
            Industry industry = ctx.getMarket().getIndustry(industryId);
            return industry.getAICoreId() != null && industry.getAICoreId().equals(aiCoreId);
        }
    }

    public static class PlanetWaterLevel extends TerraformingRequirement {
        int minWaterLevel;
        int maxWaterLevel;
        protected PlanetWaterLevel(String requirementId, boolean invert, int minWaterLevel, int maxWaterLevel) {
            super(requirementId, invert);
            this.minWaterLevel = minWaterLevel;
            this.maxWaterLevel = maxWaterLevel;
        }

        @Override
        protected boolean checkRequirementImpl(RequirementContext ctx) {
            boggledTools.PlanetType planetType = boggledTools.getPlanetType(ctx.getPlanet());
            int planetWaterLevel = planetType.getWaterLevel(ctx);
            return minWaterLevel <= planetWaterLevel && planetWaterLevel <= maxWaterLevel;
        }
    }

    public static class MarketHasWaterPresent extends PlanetWaterLevel {
        public MarketHasWaterPresent(String requirementId, boolean invert, int minWaterLevel, int maxWaterLevel) {
            super(requirementId, invert, minWaterLevel, maxWaterLevel);
        }

        @Override
        protected boolean checkRequirementImpl(RequirementContext ctx) {
            return super.checkRequirementImpl(ctx) || boggledTools.hasIsmaraSling(ctx.getMarket());
        }
    }

    public static class TerraformingPossibleOnMarket extends TerraformingRequirement {
        List<String> invalidatingConditions;
        public TerraformingPossibleOnMarket(String requirementId, boolean invert, List<String> invalidatingConditions) {
            super(requirementId, invert);
            this.invalidatingConditions = invalidatingConditions;
        }

        @Override
        protected boolean checkRequirementImpl(RequirementContext ctx) {
            for (String invalidatingCondition : invalidatingConditions) {
                if (ctx.getMarket().hasCondition(invalidatingCondition)) {
                    return false;
                }
            }
            return boggledTools.getPlanetType(ctx.getPlanet()).getTerraformingPossible();
        }
    }

    public static class MarketHasTags extends TerraformingRequirement {
        List<String> tags;
        public MarketHasTags(String requirementId, boolean invert, List<String> tags) {
            super(requirementId, invert);
            this.tags = tags;
        }

        @Override
        protected boolean checkRequirementImpl(RequirementContext ctx) {
            for (String tag : tags) {
                if (!ctx.getMarket().hasTag(tag) && !ctx.getMarket().getPrimaryEntity().hasTag(tag)) {
                    return false;
                }
            }
            return true;
        }
    }

    public static class MarketIsAtLeastSize extends TerraformingRequirement {
        int colonySize;
        public MarketIsAtLeastSize(String requirementId, boolean invert, int colonySize) {
            super(requirementId, invert);
            this.colonySize = colonySize;
        }

        @Override
        protected boolean checkRequirementImpl(RequirementContext ctx) {
            return ctx.getMarket().getSize() >= colonySize;
        }
    }

    public static class MarketStorageContainsAtLeast extends TerraformingRequirement {
        String submarketId;
        String cargoId;
        int quantity;
        public MarketStorageContainsAtLeast(String requirementId, boolean invert, String submarketId, String cargoId, int quantity) {
            super(requirementId, invert);
            this.submarketId = submarketId;
            this.cargoId = cargoId;
            this.quantity = quantity;
        }

        @Override
        public void addTokenReplacements(Map<String, String> tokenReplacements) {
            for (SubmarketSpecAPI submarketSpec : Global.getSettings().getAllSubmarketSpecs()) {
                if (!submarketSpec.getId().equals(submarketId)) {
                    continue;
                }
                tokenReplacements.put("$submarket", Misc.lcFirst(submarketSpec.getName()));
                break;
            }
            tokenReplacements.put("$cargoName", Global.getSettings().getCommoditySpec(cargoId).getLowerCaseName());
            tokenReplacements.put("$cargoQuantity", Integer.toString(quantity));
        }

        @Override
        protected boolean checkRequirementImpl(RequirementContext ctx) {
            return ctx.getMarket().getSubmarket(submarketId).getCargo().getCommodityQuantity(cargoId) >= quantity;
        }
    }

    public static class PlayerHasStoryPointsAtLeast extends TerraformingRequirement {
        int quantity;
        public PlayerHasStoryPointsAtLeast(String requirementId, boolean invert, int quantity) {
            super(requirementId, invert);
            this.quantity = quantity;
        }

        @Override
        public void addTokenReplacements(Map<String, String> tokenReplacements) {
            tokenReplacements.put("$storyPointsQuantity", Integer.toString(quantity));
        }

        @Override
        protected boolean checkRequirementImpl(RequirementContext ctx) {
            return Global.getSector().getPlayerStats().getStoryPoints() >= quantity;
        }
    }

    public static class WorldTypeSupportsResourceImprovement extends TerraformingRequirement {
        String resourceId;
        public WorldTypeSupportsResourceImprovement(String requirementId, boolean invert, String resourceId) {
            super(requirementId, invert);
            this.resourceId = resourceId;
        }

        @Override
        protected boolean checkRequirementImpl(RequirementContext ctx) {
            Pair<String, String> key = new Pair<>(boggledTools.getPlanetType(ctx.getPlanet()).getPlanetId(), resourceId);
            String maxResource = boggledTools.getResourceLimits().get(key);

            if (maxResource == null || maxResource.isEmpty()) {
                return false;
            }

            if (ctx.getMarket().hasCondition(maxResource)) {
                return false;
            }

            List<String> resourceProgression = boggledTools.getResourceProgressions().get(resourceId);

            boolean maxResourcePassed = false;
            for (String resource : resourceProgression) {
                if (resource.equals(maxResource)) {
                    maxResourcePassed = true;
                }
                if (ctx.getMarket().hasCondition(resource) && maxResourcePassed) {
                    return false;
                }
            }

            return true;
        }
    }

    public static class IntegerFromTagSubstring extends TerraformingRequirement {
        String option;
        String tagSubstring;
        int maxValue;

        public IntegerFromTagSubstring(String requirementId, boolean invert, String option, String tagSubstring, int maxValue) {
            super(requirementId, invert);
            this.tagSubstring = tagSubstring;
            this.maxValue = maxValue;
            this.option = option;
        }

        @Override
        protected boolean checkRequirementImpl(RequirementContext ctx) {
            int testValue = 0;
            if (!option.isEmpty()) {
                testValue += boggledTools.getIntSetting(option);
            }
            for (String tag : ctx.getMarket().getTags()) {
                if (tag.contains(tagSubstring)) {
                    testValue += Integer.parseInt(tag.substring(tagSubstring.length()));
                    break;
                }
            }
            return maxValue > testValue;
        }
    }

    public static class PlayerHasSkill extends TerraformingRequirement {
        String skill;
        public PlayerHasSkill(String requirementId, boolean invert, String skill) {
            super(requirementId, invert);
            this.skill = skill;
        }

        @Override
        protected boolean checkRequirementImpl(RequirementContext ctx) {
            return Global.getSector().getPlayerStats().getSkillLevel(skill) != 0;
        }
    }

    public static class SystemStarHasTags extends TerraformingRequirement {
        List<String> tags;
        public SystemStarHasTags(String requirementId, boolean invert, List<String> tags) {
            super(requirementId, invert);
            this.tags = tags;
        }

        private boolean starHasTag(PlanetAPI star, String tag) {
            return star != null && star.hasTag(tag);
        }

        @Override
        protected boolean checkRequirementImpl(RequirementContext ctx) {
            StarSystemAPI system = ctx.getMarket().getStarSystem();
            PlanetAPI primary = system.getStar();
            PlanetAPI secondary = system.getSecondary();
            PlanetAPI tertiary = system.getTertiary();
            for (String tag : tags) {
                boolean hasTag = starHasTag(primary, tag) || starHasTag(secondary, tag) || starHasTag(tertiary, tag);
                if (!hasTag) {
                    return false;
                }
            }
            return true;
        }
    }

    public static class SystemStarType extends TerraformingRequirement {
        String starType;
        public SystemStarType(String requirementId, boolean invert, String starType) {
            super(requirementId, invert);
            this.starType = starType;
        }

        private boolean starEquals(PlanetAPI star) {
            return star != null && star.getTypeId().equals(starType);
        }
        @Override
        protected boolean checkRequirementImpl(RequirementContext ctx) {
            StarSystemAPI system = ctx.getMarket().getStarSystem();
            PlanetAPI primary = system.getStar();
            PlanetAPI secondary = system.getSecondary();
            PlanetAPI tertiary = system.getTertiary();

            if (starEquals(primary)) {
                return true;
            }
            else if (starEquals(secondary)) {
                return true;
            }
            return starEquals(tertiary);
        }
    }

    public static class PlayerFleetInHyperspace extends TerraformingRequirement {
        protected PlayerFleetInHyperspace(String requirementId, boolean invert) {
            super(requirementId, invert);
        }

        @Override
        protected boolean checkRequirementImpl(RequirementContext ctx) {
            return ctx.getFleet().isInHyperspace() || ctx.getFleet().isInHyperspaceTransition();
        }
    }

    public static class SystemHasJumpPoints extends TerraformingRequirement {
        protected SystemHasJumpPoints(String requirementId, boolean invert) {
            super(requirementId, invert);
        }

        @Override
        protected boolean checkRequirementImpl(RequirementContext ctx) {
            return !ctx.getFleet().getStarSystem().getJumpPoints().isEmpty();
        }
    }

    public static class TargetPlanetNotOwnedBy extends TerraformingRequirement {
        List<String> factions;

        protected TargetPlanetNotOwnedBy(String requirementId, boolean invert) {
            super(requirementId, invert);
        }

        @Override
        protected boolean checkRequirementImpl(RequirementContext ctx) {
            SectorEntityToken targetPlanet = ctx.getPlanet();
            if (targetPlanet == null) {
                return false;
            }

            MarketAPI market = targetPlanet.getMarket();
            if (market == null) {
                return false;
            }

            if (!market.isPlayerOwned()) {
                for (String faction : factions) {
                    if (market.getFaction() != Global.getSector().getFaction(faction)) {
                        return false;
                    }
                }
            }

            return true;
        }
    }

    public static class TargetPlanetGovernedByPlayer extends TerraformingRequirement {
        protected TargetPlanetGovernedByPlayer(String requirementId, boolean invert) {
            super(requirementId, invert);
        }

        @Override
        protected boolean checkRequirementImpl(RequirementContext ctx) {
            SectorEntityToken targetPlanet = ctx.getPlanet();
            if (targetPlanet == null) {
                return false;
            }

            MarketAPI market = targetPlanet.getMarket();
            if (market == null) {
                return false;
            }

            if (!market.isPlayerOwned()) {
                return false;
            }

            if (market.getFaction() == ctx.getFleet().getFaction()) {
                return false;
            }

            return true;
        }
    }

    public static class TargetPlanetWithinDistance extends TerraformingRequirement {
        float distance;
        protected TargetPlanetWithinDistance(String requirementId, boolean invert, float distance) {
            super(requirementId, invert);
            this.distance = distance;
        }

        @Override
        protected boolean checkRequirementImpl(RequirementContext ctx) {
            SectorEntityToken targetPlanet = ctx.getPlanet();
            if (targetPlanet == null) {
                return false;
            }
            return (Misc.getDistance(ctx.getFleet(), targetPlanet) - targetPlanet.getRadius()) < distance;
        }
    }

    public static class TargetPlanetIsAtLeastSize extends TerraformingRequirement {
        float size;
        protected TargetPlanetIsAtLeastSize(String requirementId, boolean invert, float size) {
            super(requirementId, invert);
            this.size = size;
        }

        @Override
        protected boolean checkRequirementImpl(RequirementContext ctx) {
            SectorEntityToken targetPlanet = ctx.getPlanet();
            if (targetPlanet == null) {
                return false;
            }
            return targetPlanet.getRadius() >= size;
        }
    }

    public static class TargetPlanetHasOrbitBlockers extends TerraformingRequirement {

        protected TargetPlanetHasOrbitBlockers(String requirementId, boolean invert) {
            super(requirementId, invert);
        }

        @Override
        protected boolean checkRequirementImpl(RequirementContext ctx) {
            return false;
        }
    }
}
