import os
import argparse
import re
import sys
import random
import shutil
from typing import List,Dict,Union,Tuple

"""
    Created by : Xoros Lacknatas Ingradu Jernair ( Rose ) 
    Date : 06/01/2023
    Goals : Split dataset into Train, Val, test (if any) FOR YOLOv5 
    
"""

parser = argparse.ArgumentParser(description="SplitDataset into train, val, test For YOLOv5 dataset")

parser.add_argument('-dp',"--datasetpath", action="store", type=str, help="Dataset path ", required=True)
parser.add_argument("-lp","--labelpath",action="store", type=str, help="Label Path", required=True)
parser.add_argument("-nr", "--nameresult", action="store", type=str, help="parent folder name", default="splited")
parser.add_argument("-t", "--type", action="store", type=int, help="type 1 ( train, val ), type 2 (train,val,test)", required=True)
args = parser.parse_args()

DATASET_DIR : str = args.datasetpath
LABEL_DIR : str = args.labelpath
DEFAULT_SPLIT : int = 80
PARENT_FOLDER_NAME : str = args.nameresult
TYPE_RESULT : int = args.type

ALL_FILENAMES : List[str] = []

TRAIN_FILENAMES : List[str] = []
VALIDATION_FILENAMES : List[str] = []
TEST_FILENAMES : List[str] = []

def validateFolderPath():
    """
        Validate the folder path
    """

def assertFilenames(l1 : List[str], l2: List[str]) -> bool:
    """
        ASserting whether the images and & labels Files is same

    Args:
        l1 (List[str]): _description_
        l2 (List[str]): _description_

    Returns:
        bool: Obviously
    """

    for f1, f2 in zip(l1, l2):
        if(f1 != f2): return False
    
    return True

def extractFileNames():
    """
        Extract the filenames of the specified path    
        // Assumming the image name & label name same
    """
    labelsName : List[str] = []
    filesName : List[str] = []
    
    for labelName in os.listdir(LABEL_DIR):
        name = labelName.replace(".txt","")
        labelsName.append(name)
    
    for fileName in os.listdir(DATASET_DIR):
        name = fileName.replace(".jpg","") 
        filesName.append(name) 

    isSame : bool = assertFilenames(labelsName, filesName)
    if(isSame) :
        print("Labels and filenames is exactly the same")
        return labelsName, filesName
    else :
        print("Labels And filenames is Different , Quitting")
        sys.exit(0)


def createFolder():
    """
        Create the folder
    """
    if(os.path.isdir(PARENT_FOLDER_NAME) == False):
        os.mkdir(PARENT_FOLDER_NAME)
        os.mkdir(os.path.join(PARENT_FOLDER_NAME, "images"))
        os.mkdir(os.path.join(PARENT_FOLDER_NAME, "labels"))

        os.mkdir(os.path.join(PARENT_FOLDER_NAME, "images", "train"))
        os.mkdir(os.path.join(PARENT_FOLDER_NAME, "images", "validation"))

        os.mkdir(os.path.join(PARENT_FOLDER_NAME, "labels", "train"))
        os.mkdir(os.path.join(PARENT_FOLDER_NAME, "labels", "validation"))
        if(TYPE_RESULT == 2):
            os.mkdir(os.path.join(PARENT_FOLDER_NAME, "test"))        

def extractTheLabelFromText(string : str):
    """
        Extract the labels using pattern
    """
    pattern = "[^.]*"
    return re.search(pattern, string).group()

def extractIndexFromLabels(labelsParams):
    """
        If The name of file is :
        label-xxxxx.jpg
        otherwise i dont know

        return the dict of label
    """
    labels = {}
    for label in labelsParams  :
        label = extractTheLabelFromText(label)
        if(label not in labels.keys()):
            labels[label] = []
    
    for indx, label in enumerate(labelsParams):
        label = extractTheLabelFromText(label)
        labels[label].append(indx)
    
    return labels
        
def splitDataset(labels : Dict[List[int], List], splitperc : int, splitPercTest : int = 0) -> Tuple[ Dict[List[str], List], Dict[List[str], List]]:
    """
        Split Dataset according its splitPercentage

        NOt adding for Testing yet, bcs I m stupid
    Args:
        labels (Dict): Dictionary of label and its list of index
        splitperc (int): The percentage of split


        Return indexLabelTrain : List[Dict[str]], indexLabelValidation : List[Dict[str]]
    """

    labelKeys = labels.keys()
    indexLabelTrain : Dict[str, List] = {}
    indexLabelValidation : Dict[str, List] = {}

    for label in labels.keys():
 
        listOfIndx : List[int] = labels[label]
        totalTrain : int = int(len(listOfIndx) * splitperc / 100)

        # Randomize the list
        random.shuffle(listOfIndx)
        indexLabelTrain[label] = listOfIndx[:totalTrain]
        indexLabelValidation[label] = listOfIndx[totalTrain:]
        

    return indexLabelTrain, indexLabelValidation
       

def moveDataset(labelTrainIndx : Dict[str, List[int]], validationTrainIndx : Dict[str, List[int]]):
    """
        Moving dataset to its Train, and validation path

        labelTrainIndx :  Dict[str, List[int]] -> Just a corresponding label and its indx for training
        validationTrainIndx :  Dict[str, List[int]] -> Just a corresponding label and its indx for validation

        Will be adding the test,  demo ima ja arimasen, atode, itsu ? Wakarimasen 
    """
    for labelKey  in labelTrainIndx.keys() :
        labelsPlace = os.listdir(LABEL_DIR)
        imagesPlace = os.listdir(DATASET_DIR)
        for labelIndx in labelTrainIndx[labelKey]:
            pathSourceLabel = os.path.join(LABEL_DIR, labelsPlace[labelIndx])
            pathSourceDataset = os.path.join(DATASET_DIR, imagesPlace[labelIndx])

            pathDestinationLabel = os.path.join(PARENT_FOLDER_NAME, "labels", "train", labelsPlace[labelIndx])
            pathDestinationDataset = os.path.join(PARENT_FOLDER_NAME, "images", "train", imagesPlace[labelIndx])
            
            shutil.copy(pathSourceDataset, pathDestinationDataset)
            shutil.copy(pathSourceLabel, pathDestinationLabel)
        for labelIndxValidation in validationTrainIndx[labelKey]:
            ## Move the index of validation index to its folder

            pathSourceLabel = os.path.join(LABEL_DIR, labelsPlace[labelIndx])
            pathSourceDataset = os.path.join(DATASET_DIR, imagesPlace[labelIndx])

            pathDestinationLabel = os.path.join(PARENT_FOLDER_NAME, "labels","validation", labelsPlace[labelIndxValidation])
            pathDestinationDataset = os.path.join(PARENT_FOLDER_NAME, "images","validation", imagesPlace[labelIndxValidation])

            shutil.copy(pathSourceDataset, pathDestinationDataset)
            shutil.copy(pathSourceLabel, pathDestinationLabel)
    print("Moving dataset is done")

def printInformation():
    ...

def main():
    createFolder()
    labels, filenames = extractFileNames()
    labels = extractIndexFromLabels(labels)
    labelTrainIndx , labelValidationIndx = splitDataset(labels,8)
    moveDataset(labelTrainIndx, labelValidationIndx)
if __name__ == '__main__':
    # besok.7f345dd1-81a4-11ed-9bf8-c03fd5a31d89
    # python extractData.py -dp images/ -lp labels/ -nr name -t 1
    
    #labels, filenames = extractFileNames()
    #labels = extractIndexFromLabels(labels)
    #sa,sb = splitDataset(labels,8)
    #print(sa)
    #print(sb)
    main()