package boggled.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignClockAPI;
import boggled.campaign.econ.boggledTools;
import com.fs.starfarer.api.combat.MutableStat;

import java.util.*;

public class BoggledTerraformingProject {
    public static class ProjectInstance {
        private BoggledTerraformingProject project;
        private int daysCompleted = 0;
        private int lastDayChecked = 0;

        public ProjectInstance(BoggledTerraformingProject project) {
            this.project = project;
            if (Global.getSector() == null) {
                // Happens when game first loads
                this.lastDayChecked = 0;
            } else {
                this.lastDayChecked = Global.getSector().getClock().getDay();
            }
        }

        public Object readResolve() {
            BoggledTascPlugin.loadSettingsFromJSON();
            this.project = boggledTools.getProject(project.getId());
            return this;
        }

        public BoggledTerraformingProject getProject() { return project; }
        public int getDaysCompleted() { return daysCompleted; }
        public int getLastDayChecked() { return lastDayChecked; }

        public boolean advance(BoggledTerraformingRequirement.RequirementContext ctx) {
            CampaignClockAPI clock = Global.getSector().getClock();
            if (clock.getDay() == lastDayChecked) {
                return false;
            }
            lastDayChecked = clock.getDay();

            if (!project.requirementsMet(ctx)) {
                this.daysCompleted = 0;
                return false;
            }

            if (project.requirementsReset(ctx)) {
                this.daysCompleted = 0;
                return false;
            }

            if (project.requirementsStall(ctx)) {
                return false;
            }

            daysCompleted++;
            if (daysCompleted < project.getModifiedProjectDuration(ctx)) {
                return false;
            }

            project.finishProject(ctx, project.getProjectTooltip());
            return true;
        }
    }

    public static class RequirementsWithId {
        String id;
        BoggledProjectRequirementsAND requirements;

        public RequirementsWithId(String id, BoggledProjectRequirementsAND requirements) {
            this.id = id;
            this.requirements = requirements;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (object == null || getClass() != object.getClass()) return false;
            RequirementsWithId that = (RequirementsWithId) object;
            return Objects.equals(id, that.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }

    public static class RequirementAddInfo {
        String containingId;
        List<BoggledProjectRequirementsAND.RequirementAdd> requirements;

        public RequirementAddInfo(String containingId, List<BoggledProjectRequirementsAND.RequirementAdd> requirements) {
            this.containingId = containingId;
            this.requirements = requirements;
        }
    }

    public static class RequirementRemoveInfo {
        String containingId;
        List<String> requirementIds;

        public RequirementRemoveInfo(String containingId, List<String> requirementIds) {
            this.containingId = containingId;
            this.requirementIds = requirementIds;
        }
    }

    private final String id;
    private final String[] enableSettings;
    private final String projectType;
    private final String projectTooltip;
    private final String intelCompleteMessage;

    private final String incompleteMessage;
    private final List<String> incompleteMessageHighlights;
    // Multiple separate TerraformingRequirements form an AND'd collection
    // Each individual requirement inside the TerraformingRequirements forms an OR'd collection
    // ie If any of the conditions inside a TerraformingRequirements is fulfilled, that entire requirement is filled
    // But then all the TerraformingRequirements must be fulfilled for the project to be allowed
    // two is an optional description override

    private final BoggledProjectRequirementsAND requirements;
    private final BoggledProjectRequirementsAND requirementsHidden;

    private final List<RequirementsWithId> requirementsStall;
    private final List<RequirementsWithId> requirementsReset;

    private int baseProjectDuration;
    private final List<BoggledTerraformingDurationModifier.TerraformingDurationModifier> durationModifiers;

    private final List<BoggledTerraformingProjectEffect.TerraformingProjectEffect> projectCompleteEffects;
    private final List<BoggledTerraformingProjectEffect.TerraformingProjectEffect> projectOngoingEffects;

    public BoggledTerraformingProject(String id, String[] enableSettings, String projectType, String projectTooltip, String intelCompleteMessage, String incompleteMessage, List<String> incompleteMessageHighlights, BoggledProjectRequirementsAND requirements, BoggledProjectRequirementsAND requirementsHidden, List<RequirementsWithId> requirementsStall, List<RequirementsWithId> requirementsReset, int baseProjectDuration, List<BoggledTerraformingDurationModifier.TerraformingDurationModifier> durationModifiers, List<BoggledTerraformingProjectEffect.TerraformingProjectEffect> projectCompleteEffects, List<BoggledTerraformingProjectEffect.TerraformingProjectEffect> projectOngoingEffects) {
        this.id = id;
        this.enableSettings = enableSettings;
        this.projectType = projectType;
        this.projectTooltip = projectTooltip;
        this.intelCompleteMessage = intelCompleteMessage;

        this.incompleteMessage = incompleteMessage;
        this.incompleteMessageHighlights = incompleteMessageHighlights;

        this.requirements = requirements;
        this.requirementsHidden = requirementsHidden;

        this.requirementsStall = requirementsStall;
        this.requirementsReset = requirementsReset;

        this.baseProjectDuration = baseProjectDuration;
        this.durationModifiers = durationModifiers;

        this.projectCompleteEffects = projectCompleteEffects;
        this.projectOngoingEffects = projectOngoingEffects;
    }

    public String getId() { return id; }

    public String[] getEnableSettings() { return enableSettings; }

    public boolean isEnabled() { return boggledTools.optionsAllowThis(enableSettings); }

    public String getProjectType() { return projectType; }

    public String getProjectTooltip() {
        return projectTooltip;
    }

    public Map<String, BoggledTerraformingProjectEffect.EffectTooltipPara> getEffectTooltipInfo(BoggledTerraformingRequirement.RequirementContext ctx) {
        BoggledTerraformingProjectEffect.TerraformingProjectEffect.DescriptionMode descMode = BoggledTerraformingProjectEffect.TerraformingProjectEffect.DescriptionMode.TO_APPLY;
        ctx = new BoggledTerraformingRequirement.RequirementContext(ctx, this);
        Map<String, BoggledTerraformingProjectEffect.EffectTooltipPara> ret = new LinkedHashMap<>();
        for (BoggledTerraformingProjectEffect.TerraformingProjectEffect effect : projectCompleteEffects) {
            effect.addEffectTooltipInfo(ctx, ret, "Terraforming", descMode, BoggledTerraformingProjectEffect.TerraformingProjectEffect.DescriptionSource.GENERIC);
        }
        return ret;
    }

    public Map<String, BoggledTerraformingProjectEffect.EffectTooltipPara> getOngoingEffectTooltipInfo(BoggledTerraformingRequirement.RequirementContext ctx, String effectSource, BoggledTerraformingProjectEffect.TerraformingProjectEffect.DescriptionMode mode, BoggledTerraformingProjectEffect.TerraformingProjectEffect.DescriptionSource source) {
        ctx = new BoggledTerraformingRequirement.RequirementContext(ctx, this);
        Map<String, BoggledTerraformingProjectEffect.EffectTooltipPara> ret = new LinkedHashMap<>();
        for (BoggledTerraformingProjectEffect.TerraformingProjectEffect effect : projectOngoingEffects) {
            effect.addEffectTooltipInfo(ctx, ret, effectSource, mode, source);
        }
        return ret;
    }

    public String getIntelCompleteMessage() { return intelCompleteMessage; }

    public String getIncompleteMessage() { return incompleteMessage; }

    public String[] getIncompleteMessageHighlights(Map<String, String> tokenReplacements) {
        ArrayList<String> replaced = new ArrayList<>(incompleteMessageHighlights.size());
        for (String highlight : incompleteMessageHighlights) {
            replaced.add(boggledTools.doTokenReplacement(highlight, tokenReplacements));
        }
        return replaced.toArray(new String[0]);
    }

    public BoggledProjectRequirementsAND getRequirements() { return requirements; }

    public int getBaseProjectDuration() { return baseProjectDuration; }
    public int getModifiedProjectDuration(BoggledTerraformingRequirement.RequirementContext ctx) {
        MutableStat projectDuration = new MutableStat(baseProjectDuration);
        for (BoggledTerraformingDurationModifier.TerraformingDurationModifier durationModifier : durationModifiers) {
            projectDuration.applyMods(durationModifier.getDurationModifier(ctx));
        }
        return Math.max(projectDuration.getModifiedInt(), 0);
    }

    public boolean requirementsHiddenMet(BoggledTerraformingRequirement.RequirementContext ctx) {
        if (requirementsHidden == null) {
            Global.getLogger(this.getClass()).error("Terraforming hidden project requirements is null for project " + getId() + " and context " + ctx.getName());
            return false;
        }

        return requirementsHidden.requirementsMet(ctx);
    }

    public boolean requirementsMet(BoggledTerraformingRequirement.RequirementContext ctx) {
        if (requirements == null) {
            Global.getLogger(this.getClass()).error("Terraforming project requirements is null for project " + getId() + " and context " + ctx.getName());
            return false;
        }
        return requirementsHiddenMet(ctx) && requirements.requirementsMet(ctx);
    }

    public boolean requirementsStall(BoggledTerraformingRequirement.RequirementContext ctx) {
        for (RequirementsWithId requirementStall : requirementsStall) {
            if (requirementStall.requirements.requirementsMet(ctx)) {
                return true;
            }
        }
        return false;
    }

    public boolean requirementsReset(BoggledTerraformingRequirement.RequirementContext ctx) {
        for (RequirementsWithId requirementReset : requirementsReset) {
            if (requirementReset.requirements.requirementsMet(ctx)) {
                return true;
            }
        }
        return false;
    }

    public void finishProject(BoggledTerraformingRequirement.RequirementContext ctx, String effectSource) {
        ctx = new BoggledTerraformingRequirement.RequirementContext(ctx, this);
        for (BoggledTerraformingProjectEffect.TerraformingProjectEffect effect : projectCompleteEffects) {
            effect.applyProjectEffect(ctx, effectSource);
        }

        String intelTooltip = getProjectTooltip();
        String intelCompletedMessage = getIntelCompleteMessage();

        boggledTools.surveyAll(ctx.getClosestMarket());
        boggledTools.refreshSupplyAndDemand(ctx.getClosestMarket());
        boggledTools.refreshAquacultureAndFarming(ctx.getClosestMarket());

        boggledTools.showProjectCompleteIntelMessage(intelTooltip, intelCompletedMessage, ctx.getClosestMarket());
    }

    public void applyOngoingEffects(BoggledTerraformingRequirement.RequirementContext ctx, String effectSource) {
        for (BoggledTerraformingProjectEffect.TerraformingProjectEffect effect : projectOngoingEffects) {
            effect.applyProjectEffect(ctx, effectSource);
        }
    }

    public void unapplyOngoingEffects(BoggledTerraformingRequirement.RequirementContext ctx) {
        for (BoggledTerraformingProjectEffect.TerraformingProjectEffect effect : projectOngoingEffects) {
            effect.unapplyProjectEffect(ctx);
        }
    }

    private void addProjectRequirements(BoggledProjectRequirementsAND reqToModify, List<BoggledProjectRequirementsAND.RequirementAdd> reqsToAdd) {
        for (BoggledProjectRequirementsAND.RequirementAdd reqToAdd : reqsToAdd) {
            reqToModify.addRequirement(reqToAdd);
        }
    }

    private void addProjectRequirements(List<RequirementsWithId> requirements, List<RequirementAddInfo> requirementsToAdd) {
        for (RequirementAddInfo reqAddInfo : requirementsToAdd) {
            BoggledProjectRequirementsAND req = null;
            for (RequirementsWithId reqWithId : requirements) {
                if (reqWithId.id.equals(reqAddInfo.containingId)) {
                    req = reqWithId.requirements;
                }
            }
            if (req == null) {
                continue;
            }
            addProjectRequirements(req, reqAddInfo.requirements);
        }
    }

    private void removeProjectRequirements(BoggledProjectRequirementsAND reqToModify, List<String> reqsToRemove) {
        for (String reqToRemove : reqsToRemove) {
            reqToModify.removeRequirement(reqToRemove);
        }
    }

    private void removeProjectRequirements(List<RequirementsWithId> requirements, List<RequirementRemoveInfo> requirementRemoveInfo) {
        for (RequirementRemoveInfo reqRemoveInfo : requirementRemoveInfo) {
            BoggledProjectRequirementsAND req = null;
            for (RequirementsWithId reqWithId : requirements) {
                if (reqWithId.id.equals(reqRemoveInfo.containingId)) {
                    req = reqWithId.requirements;
                }
            }
            if (req == null) {
                continue;
            }
            removeProjectRequirements(req, reqRemoveInfo.requirementIds);
        }
    }

    /*
    From here on are mod helper functions
     */
    public void addRemoveProjectRequirements(List<BoggledProjectRequirementsAND.RequirementAdd> reqsAdded, List<String> reqsRemove, List<BoggledProjectRequirementsAND.RequirementAdd> reqsHiddenAdded, List<String> reqsHiddenRemove, List<RequirementAddInfo> reqsStallAdded, List<RequirementRemoveInfo> reqsStallRemove, List<RequirementAddInfo> reqsResetAdded, List<RequirementRemoveInfo> reqsResetRemove) {
        addProjectRequirements(requirements, reqsAdded);
        removeProjectRequirements(requirements, reqsRemove);

        addProjectRequirements(requirementsHidden, reqsHiddenAdded);
        removeProjectRequirements(requirementsHidden, reqsHiddenRemove);

        addProjectRequirements(requirementsStall, reqsStallAdded);
        removeProjectRequirements(requirementsStall, reqsStallRemove);

        addProjectRequirements(requirementsReset, reqsResetAdded);
        removeProjectRequirements(requirementsReset, reqsResetRemove);
    }

    public void addRemoveDurationModifiersAndDuration(Integer baseProjectDurationOverride, List<BoggledTerraformingDurationModifier.TerraformingDurationModifier> durationModifiersAdded, List<String> durationModifiersRemoved) {
        if (baseProjectDurationOverride != null) {
            baseProjectDuration = baseProjectDurationOverride;
        }

        durationModifiers.addAll(durationModifiersAdded);
        for (String durationModifierRemoved : durationModifiersRemoved) {
            int idx;
            for (idx = 0; idx < durationModifiers.size(); ++idx) {
                BoggledTerraformingDurationModifier.TerraformingDurationModifier durationModifier = durationModifiers.get(idx);
                if (durationModifier.id.equals(durationModifierRemoved)) {
                    break;
                }
            }
            if (idx == durationModifiers.size()) {
                continue;
            }
            durationModifiers.remove(idx);
        }
    }

    public void addRemoveProjectEffects(List<BoggledTerraformingProjectEffect.TerraformingProjectEffect> projectEffectsAdded, List<String> projectEffectsRemoved) {
        projectCompleteEffects.addAll(projectEffectsAdded);
        for (String projectEffectRemoved : projectEffectsRemoved) {
            int idx;
            for (idx = 0; idx < projectCompleteEffects.size(); ++idx) {
                BoggledTerraformingProjectEffect.TerraformingProjectEffect projectEffect = projectCompleteEffects.get(idx);
                if (projectEffect.id.equals(projectEffectRemoved)) {
                    break;
                }
            }
            if (idx == projectCompleteEffects.size()) {
                continue;
            }
            projectCompleteEffects.remove(idx);
        }
    }
}
