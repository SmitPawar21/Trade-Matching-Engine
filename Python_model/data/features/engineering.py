import os
import pandas as pd
# import numpy as np
import sys
from pathlib import Path
import logging
from typing import Optional

# Add current directory to path for local imports
sys.path.insert(0, str(Path(__file__).parent))

# Import local modules
from indicators import add_basic_indicators
from normalization import z_score_normalize, calculate_log_returns

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

class DataPipeline:
    def __init__(self, raw_data_dir: str, processed_data_dir: str, features_dir: str):
        """
        Initializes the Data Pipeline for training data preparation.
        """
        self.raw_data_dir = Path(raw_data_dir)
        self.processed_data_dir = Path(processed_data_dir)
        self.features_dir = Path(features_dir)
        
        # Ensure directories exist
        self.raw_data_dir.mkdir(parents=True, exist_ok=True)
        self.processed_data_dir.mkdir(parents=True, exist_ok=True)
        self.features_dir.mkdir(parents=True, exist_ok=True)

    def load_data(self, filename: str) -> pd.DataFrame:
        """Loads raw data from csv or parquet."""
        file_path = self.raw_data_dir / filename
        logger.info(f"Loading data from {file_path}")
        
        if not file_path.exists():
            raise FileNotFoundError(f"Data file not found: {file_path}")
            
        if filename.endswith('.csv'):
            return pd.read_csv(file_path)
        elif filename.endswith('.parquet'):
            return pd.read_parquet(file_path)
        else:
            raise ValueError("Unsupported file format. Use .csv or .parquet")

    def clean_data(self, df: pd.DataFrame) -> pd.DataFrame:
        """
        Cleans data: standardizes column names, sorts by time.
        """
        logger.info("Cleaning data")
        
        # Standardize column names (capitalize for pandas-ta and general consistency)
        col_map = {col: col.capitalize() for col in df.columns if col.lower() in ['open', 'high', 'low', 'close', 'volume']}
        df = df.rename(columns=col_map)
        
        # Identify time column and sort
        time_cols = [col for col in df.columns if 'time' in col.lower() or 'date' in col.lower()]
        if time_cols:
            time_col = time_cols[0]
            df[time_col] = pd.to_datetime(df[time_col])
            df = df.sort_values(by=time_col).reset_index(drop=True)
        else:
            logger.warning("No time or date column found to sort by.")
            
        return df

    def handle_missing_values(self, df: pd.DataFrame) -> pd.DataFrame:
        """Handles missing values using forward fill then backward fill."""
        logger.info("Handling missing values")
        # Forward fill first (carry forward last known value)
        df = df.ffill()
        # Backward fill for any remaining at the beginning
        df = df.bfill()
        return df

    def feature_engineering(self, df: pd.DataFrame) -> pd.DataFrame:
        """Applies technical indicators and creates new features."""
        logger.info("Engineering features (Technical Indicators)")
        # Add basic TA indicators
        df = add_basic_indicators(df)
        
        # Calculate log returns
        df = calculate_log_returns(df, column='Close')
        
        return df

    def normalize_data(self, df: pd.DataFrame) -> pd.DataFrame:
        """Normalizes numerical feature columns (excluding price and time)."""
        logger.info("Normalizing data")
        
        # Determine which columns to normalize
        # We usually avoid normalizing the exact timestamp or categorical data
        exclude_cols = [col for col in df.columns if 'time' in col.lower() or 'date' in col.lower()]
        
        # Keep raw price columns intact if needed by the environment, 
        # but normalize everything else (indicators, volume, returns)
        # Note: If environment needs normalized prices, remove from exclude list.
        exclude_cols.extend(['Open', 'High', 'Low', 'Close'])
        
        normalize_cols = [col for col in df.columns if col not in exclude_cols and pd.api.types.is_numeric_dtype(df[col])]
        
        df = z_score_normalize(df, normalize_cols)
        return df

    def run(self, filename: str) -> Optional[pd.DataFrame]:
        """
        Runs the full pipeline on a single file.
        """
        try:
            # 1. Load Data
            df = self.load_data(filename)
            
            # 2. Cleaning
            df = self.clean_data(df)
            
            # 3. Missing Value Handling
            df = self.handle_missing_values(df)
            
            # Save intermediate processed data
            processed_path = self.processed_data_dir / filename
            if filename.endswith('.csv'):
                df.to_csv(processed_path, index=False)
            else:
                df.to_parquet(processed_path, index=False)
            logger.info(f"Saved processed intermediate data to {processed_path}")
                
            # 4. Feature Engineering
            df = self.feature_engineering(df)
            
            # Feature engineering (like MA) often creates NaN values at the beginning.
            # Drop these rows.
            df = df.dropna().reset_index(drop=True)
            
            # 5. Normalization
            df = self.normalize_data(df)
            
            # 6. Save final feature set
            features_name = filename.replace('.csv', '_features.csv').replace('.parquet', '_features.parquet')
            features_path = self.features_dir / features_name
            
            if features_path.suffix == '.csv':
                df.to_csv(features_path, index=False)
            else:
                df.to_parquet(features_path, index=False)
                
            logger.info(f"Pipeline complete. Final features saved to {features_path}")
            return df
            
        except Exception as e:
            logger.error(f"Error processing {filename}: {str(e)}")
            return None

if __name__ == "__main__":
    # Get the directory of the current script to set paths relative to the project root
    current_dir = Path(__file__).parent
    base_dir = current_dir.parent.parent  # Should resolve to Python_model
    
    pipeline = DataPipeline(
        raw_data_dir=base_dir / "data" / "raw",
        processed_data_dir=base_dir / "data" / "processed",
        features_dir=base_dir / "data" / "features"
    )
    
    print(f"Pipeline initialized. Base dir: {base_dir}")
    
    # Process all CSVs in data/raw
    raw_dir = base_dir / "data" / "raw"
    if raw_dir.exists():
        csv_files = [f.name for f in raw_dir.iterdir() if f.is_file() and f.suffix == '.csv']
        if not csv_files:
            print("No CSV files found in data/raw/")
        for file in csv_files:
            print(f"Processing {file}...")
            pipeline.run(file)
    else:
        print(f"Directory {raw_dir} does not exist.")
