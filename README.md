# otp-data

## Scripts
 - `otp-setup` Download, setup, configure OTP data
 -

## Setup 
1. Clone this Git repo
2. Run `./otp-setup -h` to list options
3. Start InteractiveOtpMain in the [OTP project](https://github.com/opentripplanner/OpenTripPlanner) 
   and use the `otp/` folder in this project as the working directory.

## Adding a new configuration 

1. Create the directory in `otp/cases`
2. Use https://geojson.io/ to create a GEOJSON to use for filtering OSM data. If no 
   GoeJson is provided, the setup script will generate a link to the _norway-latest.osm.pbf_ 
   in the case folder instead. 
3. Add the case to `otp-setup` under _Special cases_:
   - In the printed menu: 
     `echo "  4) <Name of the case>"`
   - In the options list:
     `3) setup otp/cases/<case-name> "<list NeTEx namespaces>" "<list of NeTEx stop feeds>" ;;`

- The `<list NeTEx namespaces>` is used to download netex transit data from Entur.  
- The `<list of NeTEx stop feeds>` is used to download netex stops from Entur. This is only 
  needed if the transit data file does not contains stop.
