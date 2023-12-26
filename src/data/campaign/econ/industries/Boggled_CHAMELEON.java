package data.campaign.econ.industries;

import java.awt.*;
import java.lang.String;
import java.util.LinkedHashMap;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.*;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.util.Pair;
import data.campaign.econ.boggledTools;
import kotlin.Triple;
import org.json.JSONException;
import org.json.JSONObject;

public class Boggled_CHAMELEON extends BaseIndustry implements BoggledCommonIndustryInterface
{
    @Override
    public boolean canBeDisrupted() {
        return true;
    }

    public static float IMPROVE_STABILITY_BONUS = 1f;

    public static float UPKEEP_MULT = 0.75F;
    public static int DEMAND_REDUCTION = 1;

    private static BoggledCommonIndustry commonIndustry;

    public static void settingsFromJSON(JSONObject data) throws JSONException {
        commonIndustry = new BoggledCommonIndustry(data, "CHAMELEON");
    }

    @Override
    public LinkedHashMap<String, String> getTokenReplacements() {
        LinkedHashMap<String, String> tokenReplacements = new LinkedHashMap<>();
        tokenReplacements.put("market", commonIndustry.getFocusMarketOrMarket(getMarket()).getName());
        return tokenReplacements;
    }

    @Override
    public boolean isAvailableToBuild() {
        return commonIndustry.isAvailableToBuild(getMarket());
    }

    @Override
    public boolean showWhenUnavailable() {
        return commonIndustry.showWhenUnavailable(getMarket());
    }

    @Override
    public String getUnavailableReason() {
        return commonIndustry.getUnavailableReason(getMarket(), getTokenReplacements());
    }

    @Override
    public void advance(float amount)
    {
        super.advance(amount);

        commonIndustry.advance(amount, this);
    }

    @Override
    public void apply()
    {
        super.apply(true);

        if(boggledTools.getBooleanSetting(boggledTools.BoggledSettings.domainArchaeologyEnabled))
        {
            int size = this.market.getSize();
            this.demand(boggledTools.BoggledCommodities.domainArtifacts, size);
        }
    }

    @Override
    public void unapply()
    {
        super.unapply();
    }

    @Override
    public void notifyBeingRemoved(MarketAPI.MarketInteractionMode mode, boolean forUpgrade)
    {
        super.notifyBeingRemoved(mode, forUpgrade);
    }

    @Override
    protected void addRightAfterDescriptionSection(TooltipMakerAPI tooltip, IndustryTooltipMode mode)
    {
        float opad = 10.0F;
        Color highlight = Misc.getHighlightColor();

        // TODO: Hardcoded indices, hate it, fix it
        if (commonIndustry.projects.get(0).component1().requirementsMet(getMarket())) {
            commonIndustry.tooltipIncomplete(this, tooltip, mode, "Approximately %s of the decivilized subpopulation on " + this.getMarket().getName() + " has been eradicated.", opad, highlight, commonIndustry.getPercentComplete(0, this) + "%");
        }
        if (commonIndustry.projects.get(1).component1().requirementsMet(getMarket())) {
            commonIndustry.tooltipIncomplete(this, tooltip, mode, "An investigation into the whereabouts of the rogue AI core on " + getMarket().getName() + " is approximately %s complete.", opad, highlight, commonIndustry.getPercentComplete(1, this) + "%");
        }
        for (Triple<boggledTools.TerraformingProject, String, String> project : commonIndustry.projects) {
            if (project.component1().requirementsMet(getMarket())) {
                commonIndustry.tooltipDisrupted(this, tooltip, mode, "Progress is stalled while CHAMELEON is disrupted.", opad, Misc.getNegativeHighlightColor());
            }
        }
    }

    @Override
    protected boolean hasPostDemandSection(boolean hasDemand, IndustryTooltipMode mode) {
        return true;
    }

    @Override
    protected void addPostDemandSection(TooltipMakerAPI tooltip, boolean hasDemand, IndustryTooltipMode mode)
    {
        boolean shortage = false;
        if(boggledTools.getBooleanSetting(boggledTools.BoggledSettings.domainArchaeologyEnabled))
        {
            Pair<String, Integer> deficit = this.getMaxDeficit(boggledTools.BoggledCommodities.domainArtifacts);
            if(deficit.two != 0)
            {
                shortage = true;
            }
        }

        if(shortage && mode != IndustryTooltipMode.ADD_INDUSTRY && mode != IndustryTooltipMode.QUEUED && !isBuilding())
        {
            float opad = 10.0F;
            Color bad = Misc.getNegativeHighlightColor();

            Pair<String, Integer> deficit = this.getMaxDeficit(boggledTools.BoggledCommodities.domainArtifacts);
            if(deficit.two != 0)
            {
                tooltip.addPara("CHAMELEON is inactive due to a shortage of Domain-era artifacts.", bad, opad);
            }
        }
    }

    @Override
    public float getPatherInterest()
    {
        boolean shortage = false;
        if(boggledTools.getBooleanSetting(boggledTools.BoggledSettings.domainArchaeologyEnabled))
        {
            Pair<String, Integer> deficit = this.getMaxDeficit(boggledTools.BoggledCommodities.domainArtifacts);
            if(deficit.two != 0)
            {
                shortage = true;
            }
        }

        if(isFunctional() && !isBuilding() && this.aiCoreId != null && this.aiCoreId.equals(Commodities.ALPHA_CORE) && !shortage)
        {
            // Neutralizes Pather interest to zero.
            // Previously this just returned -1000, but with the 0.96a event system the large negative amount would be subtracted from the hostile event progress.
            int totalPatherInterest = 0;

            if (market.getAdmin().getAICoreId() != null)
            {
                totalPatherInterest += 10;
            }

            for(Industry industry : market.getIndustries())
            {
                if(!industry.isHidden() && !industry.getId().equals(boggledTools.BoggledIndustries.CHAMELEONIndustryId))
                {
                    float industryPatherInterest = industry.getPatherInterest();
                    if(industryPatherInterest >= 0.0)
                    {
                        totalPatherInterest += (int) industryPatherInterest;
                    }
                }
            }

            return totalPatherInterest * -1;
        }
        else
        {
            return 10.0F;
        }
    }

    @Override
    public void addAlphaCoreDescription(TooltipMakerAPI tooltip, AICoreDescriptionMode mode) {
        float opad = 10.0F;
        Color highlight = Misc.getHighlightColor();
        String pre = "Alpha-level AI core currently assigned. ";
        if (mode == AICoreDescriptionMode.MANAGE_CORE_DIALOG_LIST || mode == AICoreDescriptionMode.INDUSTRY_TOOLTIP) {
            pre = "Alpha-level AI core. ";
        }

        if (mode == AICoreDescriptionMode.INDUSTRY_TOOLTIP) {
            CommoditySpecAPI coreSpec = Global.getSettings().getCommoditySpec(this.aiCoreId);
            TooltipMakerAPI text = tooltip.beginImageWithText(coreSpec.getIconName(), 48.0F);
            text.addPara(pre + "Reduces upkeep cost by %s. Reduces demand by %s unit. " + "Pather cells on " + this.market.getName() + " are eliminated.", 0.0F, highlight, (int)((1.0F - UPKEEP_MULT) * 100.0F) + "%", "" + DEMAND_REDUCTION);
            tooltip.addImageWithText(opad);
        } else {
            tooltip.addPara(pre + "Reduces upkeep cost by %s. Reduces demand by %s unit. " + "Pather cells on " + this.market.getName() + " are eliminated.", opad, highlight, (int)((1.0F - UPKEEP_MULT) * 100.0F) + "%", "" + DEMAND_REDUCTION);
        }
    }

    @Override
    public boolean canImprove() {
        return true;
    }

    @Override
    protected void applyImproveModifiers()
    {
        if (isImproved())
        {
            market.getStability().modifyFlat("CHAMELEON_improve", IMPROVE_STABILITY_BONUS, getImprovementsDescForModifiers() + " (CHAMELEON)");
        }
        else
        {
            market.getStability().unmodifyFlat("CHAMELEON_improve");
        }
    }

    @Override
    public void addImproveDesc(TooltipMakerAPI info, ImprovementDescriptionMode mode) {
        float opad = 10f;
        Color highlight = Misc.getHighlightColor();


        if (mode == ImprovementDescriptionMode.INDUSTRY_TOOLTIP) {
            info.addPara("Stability increased by %s.", 0f, highlight, "" + (int) IMPROVE_STABILITY_BONUS);
        } else {
            info.addPara("Increases stability by %s.", 0f, highlight, "" + (int) IMPROVE_STABILITY_BONUS);
        }

        info.addSpacer(opad);
        super.addImproveDesc(info, mode);
    }
}

