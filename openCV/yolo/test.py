import torch
import os

print(
    f"Setup complete. Using torch {torch.__version__} ({torch.cuda.get_device_properties(0).name if torch.cuda.is_available() else 'CPU'})"
)

print(torch.cuda.memory_summary())
