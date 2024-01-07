package data.campaign.econ.industries;

import java.lang.String;

import com.fs.starfarer.api.campaign.econ.*;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.util.Pair;
import data.campaign.econ.boggledTools;

public class Boggled_CHAMELEON extends BoggledBaseIndustry {
    public Boggled_CHAMELEON() {
        super("chameleon");
    }

    public static float IMPROVE_STABILITY_BONUS = 1f;

    public static float UPKEEP_MULT = 0.75F;
    public static int DEMAND_REDUCTION = 1;

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
}

