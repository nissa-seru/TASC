package data.campaign.econ.industries;

import java.lang.String;

import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import data.campaign.econ.boggledTools;
import org.json.JSONException;
import org.json.JSONObject;

public class Boggled_Expand_Station extends BaseIndustry {

    private static BoggledCommonIndustry commonIndustry;

    public static void settingsFromJSON(JSONObject data) throws JSONException {
        commonIndustry = new BoggledCommonIndustry(data, "Expand Station");
    }

    @Override
    public void apply() { super.apply(true); }

    @Override
    public void unapply() {
        super.unapply();
    }

    @Override
    public void finishBuildingOrUpgrading() {
        super.finishBuildingOrUpgrading();
    }

    public float getBuildCost()
    {
        if(!boggledTools.getBooleanSetting(boggledTools.BoggledSettings.stationProgressIncreaseInCostsToExpandStation))
        {
            return this.getSpec().getCost();
        }
        else
        {
            double cost = (this.getSpec().getCost() * (Math.pow(2, boggledTools.getNumberOfStationExpansions(this.market))));
            return (float)cost;
        }
    }

    @Override
    protected void buildingFinished()
    {
        super.buildingFinished();

        boggledTools.incrementNumberOfStationExpansions(this.market);

        this.market.removeIndustry(boggledTools.BoggledIndustries.stationExpansionIndustryId,null,false);
    }

    @Override
    public void startBuilding() {
        super.startBuilding();
    }

    @Override
    public boolean isAvailableToBuild() { return commonIndustry.isAvailableToBuild(getMarket()); }

    @Override
    public boolean showWhenUnavailable() { return commonIndustry.showWhenUnavailable(getMarket()); }

    @Override
    public String getUnavailableReason() { return commonIndustry.getUnavailableReason(getMarket()); }

    public boolean canInstallAICores() {
        return false;
    }
}
