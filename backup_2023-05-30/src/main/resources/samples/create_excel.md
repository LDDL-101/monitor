# Sample Excel File Creation Instructions

## 地表沉降数据 (Ground Subsidence Data)

To create a sample Excel file for testing the ground subsidence data upload functionality:

1. Create a new Excel file (xlsx format)
2. Create a sheet named "地表点沉降" (Ground Point Subsidence)
3. Add the following headers in row 1:
   - A1: 测点编号 (Point ID)
   - B1: 本次高程 (Current Elevation)

4. Add the following sample data starting from row 2:
   | 测点编号 | 本次高程 |
   |---------|---------|
   | P001    | 102.541 |
   | P002    | 103.226 |
   | P003    | 101.965 |
   | P004    | 102.108 |
   | P005    | 103.772 |

5. Save the file as "subsidence_data.xlsx" in the src/main/resources/samples/ directory

## Usage

1. In the application, go to the Settlement Data interface
2. Click the "Upload Data" button
3. Navigate to and select this file
4. The data should be imported and displayed in the table view

## Notes

- The system will check the point IDs against configured monitoring points
- If a point isn't configured yet, it will be automatically added
- The "本次高程" (Current Elevation) values should be in meters
